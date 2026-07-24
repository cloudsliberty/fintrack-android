@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.accounts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.ACCOUNT_TYPES
import com.fintrack.android.data.model.Account
import com.fintrack.android.data.model.Currency
import com.fintrack.android.ui.common.*

/** Ledger-type groups shown for active accounts, in display order. "Inactive" is appended after these regardless of type. */
private val ACCOUNT_GROUP_ORDER = listOf("asset" to "Asset", "expense" to "Expenses", "revenue" to "Revenue", "liability" to "Liability")

@Composable
fun AccountsScreen(onOpenAccountTransactions: (Account) -> Unit) {
    val viewModel: AccountsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val currencies by viewModel.currencies.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showEditor by remember { mutableStateOf<Account?>(null) }
    var showAddNew by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Account?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    // Which groups are expanded, keyed by group label. Defaults to expanded the first time a group is seen.
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Accounts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNew = true }) { Icon(Icons.Filled.Add, contentDescription = "Add account") }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyBox("No accounts yet. Tap + to add your first one.")
                    } else {
                        val matches = remember(s.data, searchQuery) {
                            s.data.filter { fuzzyMatch(searchQuery, it.name) }
                        }
                        val grouped = remember(matches) {
                            buildList {
                                ACCOUNT_GROUP_ORDER.forEach { (type, groupLabel) ->
                                    val group = matches.filter { it.active && it.type == type }
                                    if (group.isNotEmpty()) add(groupLabel to group)
                                }
                                val inactive = matches.filter { !it.active }
                                if (inactive.isNotEmpty()) add("Inactive" to inactive)
                            }
                        }
                        // Auto-expand any group that has a match while actively searching, so results aren't hidden
                        // behind a collapsed section. Collapsed/expanded state is otherwise left as the person set it.
                        LaunchedEffect(searchQuery, grouped.map { it.first }) {
                            if (searchQuery.isNotBlank()) {
                                grouped.forEach { (label, _) -> expandedGroups[label] = true }
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search accounts") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            if (grouped.isEmpty()) {
                                EmptyBox("No accounts match \"$searchQuery\".")
                            } else {
                                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    grouped.forEach { (groupLabel, group) ->
                                        val isExpanded = expandedGroups[groupLabel] ?: true
                                        item(key = "header-$groupLabel") {
                                            AccountGroupHeader(
                                                label = groupLabel,
                                                count = group.size,
                                                expanded = isExpanded,
                                                onToggle = { expandedGroups[groupLabel] = !isExpanded }
                                            )
                                        }
                                        if (isExpanded) {
                                            items(group, key = { it.id }) { account ->
                                                AccountRow(
                                                    account = account,
                                                    onOpen = { onOpenAccountTransactions(account) },
                                                    onEdit = { showEditor = account },
                                                    onDelete = { pendingDelete = account }
                                                )
                                            }
                                        }
                                    }
                                    item { Spacer(Modifier.height(72.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNew) {
        AccountEditorDialog(
            account = null,
            currencies = currencies,
            onDismiss = { showAddNew = false },
            onSave = { name, type, currency, desc, icon, color, active ->
                viewModel.saveAccount(null, name, type, currency, desc, icon, color, active)
                showAddNew = false
            }
        )
    }
    showEditor?.let { account ->
        AccountEditorDialog(
            account = account,
            currencies = currencies,
            onDismiss = { showEditor = null },
            onSave = { name, type, currency, desc, icon, color, active ->
                viewModel.saveAccount(account.id, name, type, currency, desc, icon, color, active)
                showEditor = null
            }
        )
    }
    pendingDelete?.let { account ->
        ConfirmDialog(
            title = "Delete account?",
            message = "This deletes \"${account.name}\" and cannot be undone. Its transactions are not deleted.",
            onConfirm = { viewModel.deleteAccount(account.id) },
            onDismiss = { pendingDelete = null }
        )
    }
    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearActionError,
            title = { Text("Couldn't save") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearActionError) { Text("OK") } }
        )
    }
}

@Composable
private fun AccountGroupHeader(label: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevronRotation")
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse $label" else "Expand $label",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun AccountRow(account: Account, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = remember(account.color) { parseHexColor(account.color) }
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(account.icon.ifBlank { account.name.take(1).uppercase() }, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${account.type.replaceFirstChar { it.uppercase() }} · ${account.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!account.active) {
                AssistChip(onClick = onEdit, label = { Text("Inactive") })
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun AccountEditorDialog(
    account: Account?,
    currencies: List<Currency>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, currency: String, description: String, icon: String, color: String, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: ACCOUNT_TYPES.first()) }
    var currency by remember { mutableStateOf(account?.currency ?: currencies.firstOrNull()?.code ?: "") }
    var description by remember { mutableStateOf(account?.description ?: "") }
    var icon by remember { mutableStateOf(account?.icon ?: "") }
    var active by remember { mutableStateOf(account?.active ?: true) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    val color = account?.color ?: randomAccountColor()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "New Account" else "Edit Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = icon, onValueChange = { icon = it.take(2) }, label = { Text("Icon (emoji, optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = typeMenuOpen, onExpandedChange = { typeMenuOpen = it }) {
                    OutlinedTextField(
                        value = type.replaceFirstChar { it.uppercase() }, onValueChange = {}, readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                        ACCOUNT_TYPES.forEach { t ->
                            DropdownMenuItem(text = { Text(t.replaceFirstChar { it.uppercase() }) }, onClick = { type = t; typeMenuOpen = false })
                        }
                    }
                }

                FuzzyComboField(
                    text = currency,
                    onTextChange = { currency = it.uppercase() },
                    options = currencies.map { it.code },
                    label = "Currency",
                    allowFreeText = currencies.isEmpty()
                )
                if (currencies.isEmpty()) {
                    Text(
                        "No currencies created yet — add one under Currencies first, or type a code now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), type, currency, description.trim(), icon.trim(), color, active) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Mirrors randomLightColor() in the web app's fintrack-core.js — a random pastel hue for new accounts instead of always blue. */
private fun randomAccountColor(): String {
    val hue = (0 until 360).random()
    val color = androidx.compose.ui.graphics.Color.hsl(hue.toFloat(), 0.65f, 0.70f)
    return "#%02x%02x%02x".format((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
}

private fun parseHexColor(hex: String): androidx.compose.ui.graphics.Color = try {
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    androidx.compose.ui.graphics.Color(0xFF4F8EF7)
}
