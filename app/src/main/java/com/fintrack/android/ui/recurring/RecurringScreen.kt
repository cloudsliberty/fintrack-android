@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.Account
import com.fintrack.android.data.model.RecurringRule
import com.fintrack.android.ui.common.*

private val FREQUENCIES = listOf("daily", "weekly", "monthly", "yearly")

@Composable
fun RecurringScreen() {
    val viewModel: RecurringViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showAddNew by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf<RecurringRule?>(null) }
    var pendingDelete by remember { mutableStateOf<RecurringRule?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recurring") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddNew = true }) { Icon(Icons.Filled.Add, contentDescription = "Add recurring rule") } }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = s.data
                    if (data.rules.isEmpty()) {
                        EmptyBox("No recurring transactions set up yet.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(data.rules, key = { it.id }) { rule ->
                                RecurringRow(
                                    rule = rule,
                                    account = data.accounts.find { it.id == rule.accountId },
                                    onEdit = { showEditor = rule },
                                    onDelete = { pendingDelete = rule },
                                    onPostNow = { viewModel.postNow(rule.id) }
                                )
                            }
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }

    val data = (state as? UiState.Success)?.data
    if (showAddNew && data != null) {
        RecurringEditorDialog(rule = null, accounts = data.accounts, onDismiss = { showAddNew = false },
            onSave = { id, name, type, accountId, amount, currency, frequency, nextDate, category, description, tags, active ->
                viewModel.saveRule(id, name, type, accountId, amount, currency, frequency, nextDate, category, description, tags, active)
                showAddNew = false
            })
    }
    if (showEditor != null && data != null) {
        RecurringEditorDialog(rule = showEditor, accounts = data.accounts, onDismiss = { showEditor = null },
            onSave = { id, name, type, accountId, amount, currency, frequency, nextDate, category, description, tags, active ->
                viewModel.saveRule(id, name, type, accountId, amount, currency, frequency, nextDate, category, description, tags, active)
                showEditor = null
            })
    }
    pendingDelete?.let { rule ->
        ConfirmDialog(title = "Delete recurring rule?", message = "\"${rule.name}\" will stop generating new transactions.", onConfirm = { viewModel.deleteRule(rule.id) }, onDismiss = { pendingDelete = null })
    }
    actionError?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearActionError, title = { Text("Couldn't save") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearActionError) { Text("OK") } })
    }
}

@Composable
private fun RecurringRow(rule: RecurringRule, account: Account?, onEdit: () -> Unit, onDelete: () -> Unit, onPostNow: () -> Unit) {
    ElevatedCard(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${formatMoney(rule.amount, rule.currency)} · ${rule.frequency} · next ${rule.nextDate}" + (account?.let { " · ${it.name}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!rule.active) { AssistChip(onClick = onEdit, label = { Text("Paused") }); Spacer(Modifier.width(4.dp)) }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onPostNow) { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Post Now") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}

@Composable
private fun RecurringEditorDialog(
    rule: RecurringRule?, accounts: List<Account>, onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, type: String, accountId: Int, amount: Double, currency: String, frequency: String, nextDate: String, category: String, description: String, tags: List<String>, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(rule?.name ?: "") }
    var type by remember { mutableStateOf(rule?.type ?: "expense") }
    var accountId by remember { mutableStateOf(rule?.accountId ?: accounts.firstOrNull()?.id ?: 0) }
    var accountQuery by remember {
        mutableStateOf(accounts.find { it.id == (rule?.accountId ?: accounts.firstOrNull()?.id) }?.name ?: "")
    }
    var amountText by remember { mutableStateOf(rule?.amount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(rule?.frequency ?: "monthly") }
    var nextDate by remember { mutableStateOf(rule?.nextDate ?: "") }
    var category by remember { mutableStateOf(rule?.category ?: "") }
    var description by remember { mutableStateOf(rule?.description ?: "") }
    var tagsText by remember { mutableStateOf(rule?.tags?.joinToString(", ") ?: "") }
    var active by remember { mutableStateOf(rule?.active ?: true) }
    var frequencyMenuOpen by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == accountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) "New Recurring Rule" else "Edit Recurring Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "expense", onClick = { type = "expense" }, label = { Text("Expense") })
                    FilterChip(selected = type == "income", onClick = { type = "income" }, label = { Text("Income") })
                }
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                AccountFuzzyComboField(
                    query = accountQuery,
                    onQueryChange = { accountQuery = it },
                    accounts = accounts,
                    label = "Account",
                    onSelected = { id -> accountId = id }
                )

                ExposedDropdownMenuBox(expanded = frequencyMenuOpen, onExpandedChange = { frequencyMenuOpen = it }) {
                    OutlinedTextField(
                        value = frequency.replaceFirstChar { it.uppercase() }, onValueChange = {}, readOnly = true, label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyMenuOpen) }, modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = frequencyMenuOpen, onDismissRequest = { frequencyMenuOpen = false }) {
                        FREQUENCIES.forEach { f -> DropdownMenuItem(text = { Text(f.replaceFirstChar { it.uppercase() }) }, onClick = { frequency = f; frequencyMenuOpen = false }) }
                    }
                }

                OutlinedTextField(value = nextDate, onValueChange = { nextDate = it }, label = { Text("Next date (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && amountText.toDoubleOrNull() != null && nextDate.isNotBlank() && selectedAccount != null,
                onClick = {
                    val tags = tagsText.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                    onSave(rule?.id, name.trim(), type, accountId, amountText.toDoubleOrNull() ?: 0.0, selectedAccount?.currency ?: "USD", frequency, nextDate.trim(), category.trim(), description.trim(), tags, active)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
