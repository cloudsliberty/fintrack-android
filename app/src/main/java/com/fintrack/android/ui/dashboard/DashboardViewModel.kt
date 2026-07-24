package com.fintrack.android.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.android.data.model.Account
import com.fintrack.android.data.model.Budget
import com.fintrack.android.data.model.RecurringRule
import com.fintrack.android.data.model.Summary
import com.fintrack.android.data.model.Transaction
import com.fintrack.android.data.repository.FinTrackRepository
import com.fintrack.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategorySpend(val category: String, val total: Double)

data class BudgetProgress(val budget: Budget, val spent: Double)

data class DashboardData(
    val summary: Summary,
    // Totals below are computed from accounts/transactions in the base currency only (accounts in
    // other currencies are excluded from the headline totals to avoid silently mixing currencies —
    // FinTrack's server-side dashboard normalizes with live rates, which this lightweight client-side
    // pass intentionally does not attempt).
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val cashFlow: Double,
    val upcomingRecurring: List<RecurringRule>,
    val accounts: List<Account>,
    val activeBudgets: List<BudgetProgress>,
    val topExpenseCategories: List<CategorySpend>
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val state: StateFlow<UiState<DashboardData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        // Seed instantly from whatever's cached (if we have all five pieces) so the dashboard
        // doesn't sit blank/loading on every visit while the network catches up.
        val cachedSummary = repo.peekCache<Summary>(FinTrackRepository.KEY_SUMMARY, object : com.google.gson.reflect.TypeToken<Summary>() {}.type)
        val cachedAccounts = repo.peekCache<List<Account>>(FinTrackRepository.KEY_ACCOUNTS, repo.accountsType)
        val cachedTx = repo.peekCache<List<Transaction>>(FinTrackRepository.transactionsKey(null, null, null, null, null), repo.transactionsType)
        val cachedBudgets = repo.peekCache<List<Budget>>(FinTrackRepository.KEY_BUDGETS, object : com.google.gson.reflect.TypeToken<List<Budget>>() {}.type)
        val cachedRecurring = repo.peekCache<List<RecurringRule>>("recurring", object : com.google.gson.reflect.TypeToken<List<RecurringRule>>() {}.type)
        if (cachedSummary != null && cachedAccounts != null && cachedTx != null && cachedBudgets != null && cachedRecurring != null) {
            _state.value = UiState.Success(buildDashboardData(cachedSummary, cachedAccounts, cachedTx, cachedBudgets, cachedRecurring))
        } else if (_state.value !is UiState.Success) {
            _state.value = UiState.Loading
        }

        viewModelScope.launch {
            val summaryResult = repo.getSummary()
            val accountsResult = repo.getAccounts()
            val txResult = repo.getTransactions(limit = 500)
            val budgetsResult = repo.getBudgets()
            val recurringResult = repo.getRecurring()

            val error = listOf(summaryResult, accountsResult, txResult, budgetsResult, recurringResult).firstOrNull { it.isFailure }
            if (error != null) {
                if (_state.value !is UiState.Success) {
                    _state.value = UiState.Error(error.exceptionOrNull()?.message ?: "Failed to load dashboard")
                }
                return@launch
            }

            _state.value = UiState.Success(
                buildDashboardData(
                    summaryResult.getOrDefault(Summary()),
                    accountsResult.getOrDefault(emptyList()),
                    txResult.getOrDefault(emptyList()),
                    budgetsResult.getOrDefault(emptyList()),
                    recurringResult.getOrDefault(emptyList())
                )
            )
        }
    }

    private fun buildDashboardData(
        summary: Summary,
        accounts: List<Account>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        recurring: List<RecurringRule>
    ): DashboardData {
        val base = summary.baseCurrency

        val baseAccounts = accounts.filter { it.currency == base && it.active }
        val netByAccount = transactions
            .filter { it.currency == base }
            .groupBy { it.accountId }
            .mapValues { (_, txs) -> txs.sumOf { if (it.type == "income") it.amount else -it.amount } }

        val totalAssets = baseAccounts.filter { it.type == "asset" }.sumOf { netByAccount[it.id] ?: 0.0 }
        val totalLiabilities = baseAccounts.filter { it.type == "liability" }.sumOf { netByAccount[it.id] ?: 0.0 }
        val netWorth = totalAssets - totalLiabilities

        val baseTx = transactions.filter { it.currency == base }
        val cashFlow = baseTx.sumOf { if (it.type == "income") it.amount else -it.amount }

        val topExpenses = baseTx.filter { it.type == "expense" && it.category.isNotBlank() }
            .groupBy { it.category }
            .map { (cat, txs) -> CategorySpend(cat, txs.sumOf { it.amount }) }
            .sortedByDescending { it.total }
            .take(5)

        val upcoming = recurring.filter { it.active }.sortedBy { it.nextDate }.take(5)

        return DashboardData(
            summary = summary,
            netWorth = netWorth,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            cashFlow = cashFlow,
            upcomingRecurring = upcoming,
            accounts = accounts,
            activeBudgets = budgets.filter { it.active }.map { budget ->
                val spent = transactions
                    .filter { it.type == "expense" && it.currency == budget.currency }
                    .filter { budget.category.isBlank() || it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { it.amount }
                BudgetProgress(budget, spent)
            },
            topExpenseCategories = topExpenses
        )
    }
}
