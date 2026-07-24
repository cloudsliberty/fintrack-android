@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.transfers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.Account
import com.fintrack.android.ui.common.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransfersScreen() {
    val viewModel: TransfersViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showAddNew by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transfers") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddNew = true }) { Icon(Icons.Filled.Add, contentDescription = "Add transfer") } }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = s.data
                    if (data.transfers.isEmpty()) {
                        EmptyBox("No transfers yet. Use + to move money between your accounts.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(data.transfers, key = { it.id }) { transfer ->
                                val from = data.accounts.find { it.id == transfer.fromAccountId }
                                val to = data.accounts.find { it.id == transfer.toAccountId }
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(from?.name ?: "?")
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(horizontal = 6.dp).size(16.dp))
                                                Text(to?.name ?: "?")
                                            }
                                            Text(
                                                "${formatMoney(transfer.fromAmount, transfer.fromCurrency)} → ${formatMoney(transfer.toAmount, transfer.toCurrency)} · ${transfer.date}",
                                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { pendingDeleteId = transfer.id }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
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

    val data = (state as? UiState.Success)?.data
    if (showAddNew && data != null && data.accounts.size >= 2) {
        TransferEditorDialog(accounts = data.accounts, onDismiss = { showAddNew = false },
            onSave = { fromId, toId, fromAmount, toAmount, fromCcy, toCcy, rate, desc, date ->
                viewModel.createTransfer(fromId, toId, fromAmount, toAmount, fromCcy, toCcy, rate, desc, date)
                showAddNew = false
            })
    } else if (showAddNew) {
        AlertDialog(
            onDismissRequest = { showAddNew = false },
            title = { Text("Need two accounts") },
            text = { Text("Add at least two accounts before creating a transfer.") },
            confirmButton = { TextButton(onClick = { showAddNew = false }) { Text("OK") } }
        )
    }
    pendingDeleteId?.let { id ->
        ConfirmDialog(title = "Delete transfer?", message = "This removes the transfer and its two linked transactions.", onConfirm = { viewModel.deleteTransfer(id) }, onDismiss = { pendingDeleteId = null })
    }
    actionError?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearActionError, title = { Text("Couldn't save") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearActionError) { Text("OK") } })
    }
}

@Composable
private fun TransferEditorDialog(
    accounts: List<Account>, onDismiss: () -> Unit,
    onSave: (fromId: Int, toId: Int, fromAmount: Double, toAmount: Double, fromCcy: String, toCcy: String, rate: Double, description: String, date: String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var fromAccountId by remember { mutableStateOf(accounts[0].id) }
    var toAccountId by remember { mutableStateOf(accounts[1].id) }
    var fromQuery by remember { mutableStateOf(accounts[0].name) }
    var toQuery by remember { mutableStateOf(accounts[1].name) }
    var fromAmountText by remember { mutableStateOf("") }
    var toAmountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(dateFormat.format(Date())) }

    val fromAccount = accounts.find { it.id == fromAccountId }
    val toAccount = accounts.find { it.id == toAccountId }
    val sameCurrency = fromAccount?.currency == toAccount?.currency

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Transfer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountFuzzyComboField(
                    query = fromQuery, onQueryChange = { fromQuery = it },
                    accounts = accounts,
                    label = "From account", onSelected = { id -> fromAccountId = id }
                )
                AccountFuzzyComboField(
                    query = toQuery, onQueryChange = { toQuery = it },
                    accounts = accounts,
                    label = "To account", onSelected = { id -> toAccountId = id }
                )

                OutlinedTextField(
                    value = fromAmountText,
                    onValueChange = { fromAmountText = it; if (sameCurrency) toAmountText = it },
                    label = { Text("Amount (${fromAccount?.currency ?: ""})") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (!sameCurrency) {
                    OutlinedTextField(
                        value = toAmountText, onValueChange = { toAmountText = it },
                        label = { Text("Received amount (${toAccount?.currency ?: ""})") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val fromAmount = fromAmountText.toDoubleOrNull()
            val toAmount = if (sameCurrency) fromAmount else toAmountText.toDoubleOrNull()
            TextButton(
                enabled = fromAmount != null && toAmount != null && fromAccountId != toAccountId,
                onClick = {
                    val rate = if (fromAmount != null && fromAmount != 0.0 && toAmount != null) toAmount / fromAmount else 1.0
                    onSave(fromAccountId, toAccountId, fromAmount ?: 0.0, toAmount ?: 0.0, fromAccount?.currency ?: "USD", toAccount?.currency ?: "USD", rate, description.trim(), date.trim())
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
