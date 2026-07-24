package com.fintrack.android.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.android.data.model.Account
import com.fintrack.android.data.model.AccountRequest
import com.fintrack.android.data.model.Currency
import com.fintrack.android.data.repository.FinTrackRepository
import com.fintrack.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<List<Account>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Account>>> = _state.asStateFlow()

    // Only currencies the person has actually created — no hardcoded preset list.
    private val _currencies = MutableStateFlow<List<Currency>>(emptyList())
    val currencies: StateFlow<List<Currency>> = _currencies.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init { load() }

    fun load() {
        // Show whatever was last cached right away — only fall back to a full-screen spinner if
        // we truly have nothing to show yet. The network call below still runs either way and
        // will replace this with the live result (or the same cache, on failure).
        val cached = repo.peekCache<List<Account>>(FinTrackRepository.KEY_ACCOUNTS, repo.accountsType)
        _state.value = when {
            cached != null -> UiState.Success(cached)
            _state.value !is UiState.Success -> UiState.Loading
            else -> _state.value
        }
        viewModelScope.launch {
            repo.getAccounts().fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { if (cached == null) _state.value = UiState.Error(it.message ?: "Failed to load accounts") }
            )
            repo.getCurrencies().onSuccess { _currencies.value = it }
        }
    }

    fun saveAccount(id: Int?, name: String, type: String, currency: String, description: String, icon: String, color: String, active: Boolean) {
        viewModelScope.launch {
            val body = AccountRequest(name, type, currency, description, icon, color, active)
            val result = if (id == null) repo.createAccount(body) else repo.updateAccount(id, body)
            result.fold(
                onSuccess = { load() },
                onFailure = { _actionError.value = it.message ?: "Failed to save account" }
            )
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            repo.deleteAccount(id).fold(
                onSuccess = { load() },
                onFailure = { _actionError.value = it.message ?: "Failed to delete account" }
            )
        }
    }

    fun clearActionError() { _actionError.value = null }
}
