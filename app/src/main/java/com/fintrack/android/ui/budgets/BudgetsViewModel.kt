package com.fintrack.android.ui.budgets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.android.data.model.Budget
import com.fintrack.android.data.model.BudgetRequest
import com.fintrack.android.data.repository.FinTrackRepository
import com.fintrack.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BudgetsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<List<Budget>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Budget>>> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init { load() }

    fun load() {
        val cached = repo.peekCache<List<Budget>>(FinTrackRepository.KEY_BUDGETS, object : com.google.gson.reflect.TypeToken<List<Budget>>() {}.type)
        _state.value = when {
            cached != null -> UiState.Success(cached)
            _state.value !is UiState.Success -> UiState.Loading
            else -> _state.value
        }
        viewModelScope.launch {
            repo.getBudgets().fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { if (cached == null) _state.value = UiState.Error(it.message ?: "Failed to load budgets") }
            )
        }
    }

    fun saveBudget(id: Int?, name: String, limit: Double, currency: String, period: String, category: String, active: Boolean) {
        viewModelScope.launch {
            val body = BudgetRequest(name, limit, currency, period, category, active)
            val result = if (id == null) repo.createBudget(body) else repo.updateBudget(id, body)
            result.fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to save budget" })
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            repo.deleteBudget(id).fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to delete budget" })
        }
    }

    fun clearActionError() { _actionError.value = null }
}
