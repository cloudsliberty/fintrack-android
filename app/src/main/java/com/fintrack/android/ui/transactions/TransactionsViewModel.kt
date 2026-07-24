package com.fintrack.android.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.android.data.model.*
import com.fintrack.android.data.repository.FinTrackRepository
import com.fintrack.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransactionsScreenData(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val tags: List<String>
)

/** Everything the person can narrow the transaction list by. */
data class TransactionFilters(
    val accountId: Int? = null,
    val description: String? = null,
    val category: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val tags: Set<String> = emptySet()
)

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<TransactionsScreenData>>(UiState.Loading)
    val state: StateFlow<UiState<TransactionsScreenData>> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _filters = MutableStateFlow(TransactionFilters())
    val filters: StateFlow<TransactionFilters> = _filters.asStateFlow()

    // Kept for backwards-compat call sites (account filter chip row).
    val accountFilter: Int? get() = _filters.value.accountId

    init { load() }

    fun load() {
        val f = _filters.value
        val cachedTx = repo.peekCache<List<Transaction>>(FinTrackRepository.transactionsKey(f.accountId, null, f.category, f.dateFrom, f.dateTo), repo.transactionsType)
        val cachedAccounts = repo.peekCache<List<Account>>(FinTrackRepository.KEY_ACCOUNTS, repo.accountsType)
        if (cachedTx != null || cachedAccounts != null) {
            val filteredCached = applyClientSideFilters(cachedTx ?: emptyList(), f)
            _state.value = UiState.Success(
                TransactionsScreenData(
                    transactions = filteredCached,
                    accounts = cachedAccounts ?: (state.value as? UiState.Success)?.data?.accounts ?: emptyList(),
                    categories = (state.value as? UiState.Success)?.data?.categories ?: emptyList(),
                    tags = (state.value as? UiState.Success)?.data?.tags ?: emptyList()
                )
            )
        } else if (_state.value !is UiState.Success) {
            _state.value = UiState.Loading
        }

        viewModelScope.launch {
            val txResult = repo.getTransactions(accountId = f.accountId, category = f.category, from = f.dateFrom, to = f.dateTo)
            val accResult = repo.getAccounts()
            val catResult = repo.getCategories()
            val tagsResult = repo.getTags()

            val error = listOf(txResult, accResult, catResult, tagsResult).firstOrNull { it.isFailure }
            if (error != null) {
                if (_state.value !is UiState.Success) {
                    _state.value = UiState.Error(error.exceptionOrNull()?.message ?: "Failed to load transactions")
                }
                return@launch
            }
            val allTx = txResult.getOrDefault(emptyList())
            val filteredTx = applyClientSideFilters(allTx, f)
            _state.value = UiState.Success(
                TransactionsScreenData(
                    transactions = filteredTx,
                    accounts = accResult.getOrDefault(emptyList()),
                    categories = catResult.getOrDefault(emptyList()),
                    tags = tagsResult.getOrDefault(emptyList())
                )
            )
        }
    }

    /** Refreshes accounts/categories/tags/transactions — call whenever the screen becomes visible again. */
    fun refresh() = load()

    /**
     * Filters the API isn't asked to do server-side: free-text description search (case-insensitive
     * substring) and tag matching. Account/category/date range are already applied via the API call.
     */
    private fun applyClientSideFilters(transactions: List<Transaction>, f: TransactionFilters): List<Transaction> {
        var result = transactions
        if (f.tags.isNotEmpty()) result = result.filter { tx -> tx.tags.any { it in f.tags } }
        if (!f.description.isNullOrBlank()) {
            val query = f.description.trim()
            result = result.filter { it.description.contains(query, ignoreCase = true) }
        }
        return result
    }

    fun setAccountFilter(accountId: Int?) {
        _filters.value = _filters.value.copy(accountId = accountId)
        load()
    }

    fun setFilters(filters: TransactionFilters) {
        _filters.value = filters
        load()
    }

    fun saveTransaction(
        id: Int?, accountId: Int, type: String, amount: Double, currency: String,
        description: String, category: String, tags: List<String>, notes: String, date: String
    ) {
        viewModelScope.launch {
            val body = TransactionRequest(
                accountId = accountId, type = type, amount = amount, currency = currency,
                description = description, category = category, tags = tags, notes = notes, date = date
            )
            val result = if (id == null) repo.createTransaction(body) else repo.updateTransaction(id, body)
            result.fold(
                onSuccess = { load() },
                onFailure = { _actionError.value = it.message ?: "Failed to save transaction" }
            )
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repo.deleteTransaction(id).fold(
                onSuccess = { load() },
                onFailure = { _actionError.value = it.message ?: "Failed to delete transaction" }
            )
        }
    }

    fun clearActionError() { _actionError.value = null }
}
