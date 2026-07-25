package com.fintrack.android.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.Account
import com.fintrack.android.data.model.Category
import com.fintrack.android.data.model.Transaction
import com.fintrack.android.ui.common.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(initialAccountId: Int? = null) {
    val viewModel: TransactionsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showAddNew by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    // Arriving here from "tap an account" (Accounts tab): reset every other filter and show only
    // that account's transactions. Keyed on initialAccountId so a fresh drill-down (e.g. tapping a
    // different account) re-applies even if this composable instance is reused.
    LaunchedEffect(initialAccountId) {
        if (initialAccountId != null) {
            viewModel.setFilters(TransactionFilters(accountId = initialAccountId))
        }
    }

    // Accounts/categories can change on other tabs (Accounts, Categories) while this screen stays
    // alive in the bottom-nav back stack, so refresh everything whenever we come back on screen —
    // this is what fixes "accounts/categories don't show up after being added elsewhere".
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNew = true }) { Icon(Icons.Filled.Add, contentDescription = "Add transaction") }
        }
    ) { padding ->
        val syncState by com.fintrack.android.data.SyncStatusManager.state.collectAsState()
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = syncState is com.fintrack.android.data.SyncState.Refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = s.data
                    Column(Modifier.fillMaxSize()) {
                        TransactionFiltersPanel(
                            accounts = data.accounts,
                            categories = data.categories,
                            allTags = data.tags,
                            filters = filters,
                            onFiltersChange = viewModel::setFilters
                        )
                        TransactionSummaryBar(data.transactions)
                        if (data.transactions.isEmpty()) {
                            EmptyBox("No transactions yet. Tap + to add one.")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(data.transactions, key = { it.id }) { tx ->
                                    TransactionRow(
                                        tx = tx,
                                        account = data.accounts.find { it.id == tx.accountId },
                                        onClick = { showEditor = tx },
                                        onDelete = { pendingDelete = tx }
                                    )
                                }
                                item { Spacer(Modifier.height(72.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    val data = (state as? UiState.Success)?.data
    if (showAddNew && data != null) {
        TransactionEditorDialog(
            transaction = null, accounts = data.accounts, categories = data.categories, allTags = data.tags,
            onDismiss = { showAddNew = false },
            onSave = { id, accountId, type, amount, currency, description, category, tags, notes, date ->
                viewModel.saveTransaction(id, accountId, type, amount, currency, description, category, tags, notes, date)
                showAddNew = false
            }
        )
    }
    if (showEditor != null && data != null) {
        TransactionEditorDialog(
            transaction = showEditor, accounts = data.accounts, categories = data.categories, allTags = data.tags,
            onDismiss = { showEditor = null },
            onSave = { id, accountId, type, amount, currency, description, category, tags, notes, date ->
                viewModel.saveTransaction(id, accountId, type, amount, currency, description, category, tags, notes, date)
                showEditor = null
            }
        )
    }
    pendingDelete?.let { tx ->
        ConfirmDialog(
            title = "Delete transaction?",
            message = "\"${tx.description.ifBlank { "Untitled" }}\" will be moved to the recycle bin.",
            onConfirm = { viewModel.deleteTransaction(tx.id) },
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

/** Income / Expense / Difference, color-coded green/red/orange. Split per-currency when the filtered set spans more than one, so amounts are never summed across currencies. */
@Composable
private fun TransactionSummaryBar(transactions: List<Transaction>) {
    if (transactions.isEmpty()) return
    val byCurrency = transactions.groupBy { it.currency }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        byCurrency.forEach { (currency, txs) ->
            val income = txs.filter { it.type == "income" }.sumOf { it.amount }
            val expense = txs.filter { it.type == "expense" }.sumOf { it.amount }
            val diff = income - expense
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryStat("Income", formatMoney(income, currency), com.fintrack.android.ui.theme.FtIncome)
                    SummaryStat("Expense", formatMoney(expense, currency), com.fintrack.android.ui.theme.FtExpense)
                    SummaryStat("Difference", formatMoney(diff, currency), androidx.compose.ui.graphics.Color(0xFFF59E0B))
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Accounts stays always visible per the design brief; date/category/tags live in a collapsible
 * section beneath it so the common case (just switch account) doesn't need an extra tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFiltersPanel(
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<String>,
    filters: TransactionFilters,
    onFiltersChange: (TransactionFilters) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var accountQuery by remember(filters.accountId, accounts) {
        mutableStateOf(accounts.find { it.id == filters.accountId }?.name ?: "")
    }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    AccountFuzzyComboField(
                        query = accountQuery,
                        onQueryChange = { accountQuery = it; if (it.isBlank()) onFiltersChange(filters.copy(accountId = null)) },
                        accounts = accounts,
                        label = "Account (all if empty)",
                        onSelected = { id -> onFiltersChange(filters.copy(accountId = id)) }
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Hide filters" else "More filters"
                    )
                }
            }

            // "Clear" only acts while the filter panel is expanded, per design — collapsed, the
            // account field alone is meant to stay a quick one-tap switch, not a reset control.
            if (expanded) {
                TextButton(
                    onClick = {
                        accountQuery = ""
                        onFiltersChange(TransactionFilters())
                    },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Clear all filters") }

                HorizontalDivider()

                OutlinedTextField(
                    value = filters.description ?: "",
                    onValueChange = { onFiltersChange(filters.copy(description = it.ifBlank { null })) },
                    label = { Text("Search description") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (!filters.description.isNullOrEmpty()) {
                            IconButton(onClick = { onFiltersChange(filters.copy(description = null)) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                var categoryQuery by remember(filters.category) { mutableStateOf(filters.category ?: "") }
                FuzzyComboField(
                    text = categoryQuery,
                    onTextChange = { categoryQuery = it; onFiltersChange(filters.copy(category = it.ifBlank { null })) },
                    options = categories.map { it.name },
                    label = "Category (all if empty)"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = filters.dateFrom ?: "", onValueChange = {}, readOnly = true, label = { Text("From") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Transparent tap-catcher on top: a readOnly field's own touch handling can
                        // otherwise swallow the tap before Modifier.clickable ever sees it.
                        Box(modifier = Modifier.matchParentSize().clickable { showFromPicker = true })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = filters.dateTo ?: "", onValueChange = {}, readOnly = true, label = { Text("To") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showToPicker = true })
                    }
                }

                FuzzyTagPicker(
                    selectedTags = filters.tags.toList(),
                    allTags = allTags,
                    onTagsChange = { onFiltersChange(filters.copy(tags = it.toSet())) },
                    label = "Filter by tag"
                )
            }
        }
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = filters.dateFrom?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() })
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onFiltersChange(filters.copy(dateFrom = dateFormat.format(Date(it)))) }; showFromPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
    if (showToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = filters.dateTo?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() })
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onFiltersChange(filters.copy(dateTo = dateFormat.format(Date(it)))) }; showToPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, account: Account?, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = tx.type == "income"
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.description.ifBlank { tx.category.ifBlank { "Untitled" } }, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(tx.date, account?.name, tx.category.ifBlank { null }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tx.tags.isNotEmpty()) {
                    Text(
                        tx.tags.joinToString(", ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                (if (isIncome) "+" else "-") + formatMoney(tx.amount, tx.currency),
                color = if (isIncome) com.fintrack.android.ui.theme.FtIncome else com.fintrack.android.ui.theme.FtExpense,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditorDialog(
    transaction: Transaction?,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (id: Int?, accountId: Int, type: String, amount: Double, currency: String, description: String, category: String, tags: List<String>, notes: String, date: String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var type by remember { mutableStateOf(transaction?.type ?: "expense") }
    var accountId by remember { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id ?: 0) }
    var accountQuery by remember { mutableStateOf(accounts.find { it.id == (transaction?.accountId ?: accounts.firstOrNull()?.id) }?.name ?: "") }
    var amountText by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transaction?.description ?: "") }
    var category by remember { mutableStateOf(transaction?.category ?: "") }
    var tags by remember { mutableStateOf(transaction?.tags ?: emptyList()) }
    var notes by remember { mutableStateOf(transaction?.notes ?: "") }
    var date by remember { mutableStateOf(transaction?.date ?: dateFormat.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == accountId }
    val filteredCategories = categories.filter { it.type == type }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "New Transaction" else "Edit Transaction") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "expense", onClick = { type = "expense"; category = "" }, label = { Text("Expense") })
                    FilterChip(selected = type == "income", onClick = { type = "income"; category = "" }, label = { Text("Income") })
                }

                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                AccountFuzzyComboField(
                    query = accountQuery,
                    onQueryChange = { accountQuery = it },
                    accounts = accounts,
                    label = "Account",
                    onSelected = { id -> accountId = id }
                )

                OutlinedTextField(
                    value = description, onValueChange = { description = it }, label = { Text("Description") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                // Single-line, fuzzy-filtered category field — filters as you type instead of
                // showing the full unfiltered list.
                FuzzyComboField(
                    text = category,
                    onTextChange = { category = it },
                    options = filteredCategories.map { it.name },
                    label = "Category"
                )

                FuzzyTagPicker(
                    selectedTags = tags,
                    allTags = allTags,
                    onTagsChange = { tags = it }
                )

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

                Box {
                    OutlinedTextField(
                        value = date, onValueChange = {}, readOnly = true, label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Transparent tap-catcher on top: readOnly fields can otherwise swallow the
                    // click before it reaches an outer Modifier.clickable, which is why the date
                    // picker previously never opened.
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountText.toDoubleOrNull() != null && selectedAccount != null,
                onClick = {
                    onSave(
                        transaction?.id, accountId, type, amountText.toDoubleOrNull() ?: 0.0,
                        selectedAccount?.currency ?: "USD", description.trim(), category.trim(), tags, notes.trim(), date
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val initialMillis = try { dateFormat.parse(date)?.time } catch (e: Exception) { null } ?: System.currentTimeMillis()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = dateFormat.format(Date(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}
