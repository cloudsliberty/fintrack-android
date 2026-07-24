@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.Budget
import com.fintrack.android.ui.common.*

private val PERIODS = listOf("weekly", "monthly", "yearly")

@Composable
fun BudgetsScreen() {
    val viewModel: BudgetsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showAddNew by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf<Budget?>(null) }
    var pendingDelete by remember { mutableStateOf<Budget?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budgets") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddNew = true }) { Icon(Icons.Filled.Add, contentDescription = "Add budget") } }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyBox("No budgets yet. Tap + to set a spending limit.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(s.data, key = { it.id }) { budget ->
                                BudgetRow(budget, onEdit = { showEditor = budget }, onDelete = { pendingDelete = budget })
                            }
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showAddNew) {
        BudgetEditorDialog(budget = null, onDismiss = { showAddNew = false }, onSave = { name, limit, currency, period, category, active ->
            viewModel.saveBudget(null, name, limit, currency, period, category, active); showAddNew = false
        })
    }
    showEditor?.let { budget ->
        BudgetEditorDialog(budget = budget, onDismiss = { showEditor = null }, onSave = { name, limit, currency, period, category, active ->
            viewModel.saveBudget(budget.id, name, limit, currency, period, category, active); showEditor = null
        })
    }
    pendingDelete?.let { budget ->
        ConfirmDialog(title = "Delete budget?", message = "\"${budget.name}\" will be removed.", onConfirm = { viewModel.deleteBudget(budget.id) }, onDismiss = { pendingDelete = null })
    }
    actionError?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearActionError, title = { Text("Couldn't save") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearActionError) { Text("OK") } })
    }
}

@Composable
private fun BudgetRow(budget: Budget, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(budget.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatMoney(budget.limitAmount, budget.currency)} / ${budget.period}" + (if (budget.category.isNotBlank()) " · ${budget.category}" else ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!budget.active) { AssistChip(onClick = onEdit, label = { Text("Inactive") }); Spacer(Modifier.width(4.dp)) }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun BudgetEditorDialog(
    budget: Budget?, onDismiss: () -> Unit,
    onSave: (name: String, limit: Double, currency: String, period: String, category: String, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(budget?.name ?: "") }
    var limitText by remember { mutableStateOf(budget?.limitAmount?.toString() ?: "") }
    var currency by remember { mutableStateOf(budget?.currency ?: "USD") }
    var period by remember { mutableStateOf(budget?.period ?: "monthly") }
    var category by remember { mutableStateOf(budget?.category ?: "") }
    var active by remember { mutableStateOf(budget?.active ?: true) }
    var periodMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "New Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("Limit amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currency, onValueChange = { currency = it.uppercase().take(3) }, label = { Text("Currency") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = periodMenuOpen, onExpandedChange = { periodMenuOpen = it }) {
                    OutlinedTextField(
                        value = period.replaceFirstChar { it.uppercase() }, onValueChange = {}, readOnly = true, label = { Text("Period") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = periodMenuOpen, onDismissRequest = { periodMenuOpen = false }) {
                        PERIODS.forEach { p -> DropdownMenuItem(text = { Text(p.replaceFirstChar { it.uppercase() }) }, onClick = { period = p; periodMenuOpen = false }) }
                    }
                }

                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (optional, blank = overall)") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && limitText.toDoubleOrNull() != null, onClick = {
                onSave(name.trim(), limitText.toDoubleOrNull() ?: 0.0, currency, period, category.trim(), active)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
