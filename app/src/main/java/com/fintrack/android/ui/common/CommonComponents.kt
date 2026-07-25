package com.fintrack.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fintrack.android.R
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FinTrackLoadingAnimation(size = 120.dp)
    }
}

/** Subsequence-based fuzzy match: every character of [query] must appear, in order, inside [target] (case-insensitive). */
fun fuzzyMatch(query: String, target: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    val t = target.lowercase()
    var qi = 0
    for (c in t) {
        if (qi < q.length && c == q[qi]) qi++
    }
    return qi == q.length
}

/**
 * A single-line, typeable combo box: shows a filtered (fuzzy-matched) dropdown of [options] as the
 * person types, but also accepts free text if [allowFreeText] is true. Used anywhere a preset list
 * (accounts, categories) should still be quick to pick from without forcing an exact tap-only dropdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuzzyComboField(
    text: String,
    onTextChange: (String) -> Unit,
    options: List<String>,
    label: String,
    modifier: Modifier = Modifier,
    onOptionSelected: ((String) -> Unit)? = null,
    allowFreeText: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text)) }
    var wasFocused by remember { mutableStateOf(false) }

    // Keep in sync when the caller changes the value from outside (e.g. a fresh selection elsewhere).
    androidx.compose.runtime.LaunchedEffect(text) {
        if (text != fieldValue.text) fieldValue = androidx.compose.ui.text.input.TextFieldValue(text)
    }

    val filtered = remember(fieldValue.text, options) { options.filter { fuzzyMatch(fieldValue.text, it) }.take(30) }

    fun commit(newValue: androidx.compose.ui.text.input.TextFieldValue) {
        fieldValue = newValue
        onTextChange(newValue.text)
        expanded = true
    }

    ExposedDropdownMenuBox(expanded = expanded && filtered.isNotEmpty(), onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { commit(it) },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fieldValue.text.isNotEmpty()) {
                        IconButton(onClick = { commit(androidx.compose.ui.text.input.TextFieldValue("")) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth().menuAnchor()
                .onFocusChanged { focusState ->
                    // Auto-select the existing text the moment the field gains focus, so the person
                    // can just start typing to search instead of having to manually clear it first.
                    if (focusState.isFocused && !wasFocused) {
                        fieldValue = fieldValue.copy(selection = androidx.compose.ui.text.TextRange(0, fieldValue.text.length))
                        expanded = true
                    }
                    wasFocused = focusState.isFocused
                }
        )
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filtered.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            fieldValue = androidx.compose.ui.text.input.TextFieldValue(option, selection = androidx.compose.ui.text.TextRange(option.length))
                            onTextChange(option)
                            onOptionSelected?.invoke(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Account picker used everywhere an account is chosen (transaction entry, filters, transfers,
 * recurring rules): fuzzy-searches by name, and groups results into Asset / Expenses / Revenue /
 * Liability / Inactive so long account lists stay scannable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFuzzyComboField(
    query: String,
    onQueryChange: (String) -> Unit,
    accounts: List<com.fintrack.android.data.model.Account>,
    label: String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupOrder = listOf("asset" to "Asset", "expense" to "Expenses", "revenue" to "Revenue", "liability" to "Liability")
    var expanded by remember { mutableStateOf(false) }
    var fieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(query)) }
    var wasFocused by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(query) {
        if (query != fieldValue.text) fieldValue = androidx.compose.ui.text.input.TextFieldValue(query)
    }

    val matches = remember(fieldValue.text, accounts) { accounts.filter { fuzzyMatch(fieldValue.text, it.name) } }
    val grouped = remember(matches) {
        buildList {
            groupOrder.forEach { (type, groupLabel) ->
                val group = matches.filter { it.active && it.type == type }
                if (group.isNotEmpty()) add(groupLabel to group)
            }
            val inactive = matches.filter { !it.active }
            if (inactive.isNotEmpty()) add("Inactive" to inactive)
        }
    }

    fun commit(newValue: androidx.compose.ui.text.input.TextFieldValue) {
        fieldValue = newValue
        onQueryChange(newValue.text)
        expanded = true
    }

    ExposedDropdownMenuBox(expanded = expanded && grouped.isNotEmpty(), onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { commit(it) },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fieldValue.text.isNotEmpty()) {
                        IconButton(onClick = { commit(androidx.compose.ui.text.input.TextFieldValue("")) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth().menuAnchor()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !wasFocused) {
                        fieldValue = fieldValue.copy(selection = androidx.compose.ui.text.TextRange(0, fieldValue.text.length))
                        expanded = true
                    }
                    wasFocused = focusState.isFocused
                }
        )
        if (grouped.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                grouped.forEach { (groupLabel, group) ->
                    Text(
                        groupLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 2.dp)
                    )
                    group.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.name} (${acc.currency})") },
                            onClick = {
                                fieldValue = androidx.compose.ui.text.input.TextFieldValue(acc.name, selection = androidx.compose.ui.text.TextRange(acc.name.length))
                                onQueryChange(acc.name)
                                onSelected(acc.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Same fuzzy-typeahead behavior as [FuzzyComboField], but resolves the typed text to one of [labeledOptions]' ids. */
@Composable
fun <T> FuzzyIdComboField(
    query: String,
    onQueryChange: (String) -> Unit,
    labeledOptions: List<Pair<T, String>>,
    label: String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FuzzyComboField(
        text = query,
        onTextChange = onQueryChange,
        options = labeledOptions.map { it.second },
        label = label,
        allowFreeText = false,
        onOptionSelected = { picked ->
            labeledOptions.firstOrNull { it.second == picked }?.let { onSelected(it.first) }
        },
        modifier = modifier
    )
}

/**
 * Chip-based multi-select for tags: existing tags are shown as removable chips, and a fuzzy
 * typeahead field beneath lets the person pick more from [allTags] or type a brand-new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuzzyTagPicker(
    selectedTags: List<String>,
    allTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Tags"
) {
    var input by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(input, allTags, selectedTags) {
        allTags.filter { it !in selectedTags && fuzzyMatch(input, it) }.take(20)
    }

    fun addTag(raw: String) {
        val tag = raw.trim().lowercase()
        if (tag.isNotBlank() && tag !in selectedTags) onTagsChange(selectedTags + tag)
        input = ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (selectedTags.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(selectedTags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)) {
                            Text("#$tag", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { onTagsChange(selectedTags - tag) }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove $tag", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        ExposedDropdownMenuBox(expanded = expanded && suggestions.isNotEmpty(), onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; expanded = true },
                label = { Text(label) },
                singleLine = true,
                supportingText = { Text("Type to search or add a new tag") },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            if (suggestions.isNotEmpty()) {
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    suggestions.forEach { tag ->
                        DropdownMenuItem(text = { Text("#$tag") }, onClick = { addTag(tag); expanded = false })
                    }
                }
            }
        }
        if (input.isNotBlank()) {
            TextButtonRow(label = "Add \"$input\" as a new tag", onClick = { addTag(input) })
        }
    }
}

@Composable
private fun TextButtonRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp)
    )
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun EmptyBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Formats an amount using the given ISO currency code, falling back gracefully for unknown codes. */
fun formatMoney(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        String.format(Locale.getDefault(), "%.2f %s", amount, currencyCode)
    }
}
