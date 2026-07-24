package com.fintrack.android.ui.recurring

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

data class RecurringScreenData(val rules: List<RecurringRule>, val accounts: List<Account>)

class RecurringViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<RecurringScreenData>>(UiState.Loading)
    val state: StateFlow<UiState<RecurringScreenData>> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init { load() }

    fun load() {
        val cachedRules = repo.peekCache<List<RecurringRule>>(FinTrackRepository.KEY_RECURRING, object : com.google.gson.reflect.TypeToken<List<RecurringRule>>() {}.type)
        val cachedAccounts = repo.peekCache<List<Account>>(FinTrackRepository.KEY_ACCOUNTS, repo.accountsType)
        if (cachedRules != null || cachedAccounts != null) {
            _state.value = UiState.Success(RecurringScreenData(cachedRules ?: emptyList(), cachedAccounts ?: emptyList()))
        } else if (_state.value !is UiState.Success) {
            _state.value = UiState.Loading
        }
        viewModelScope.launch {
            val rulesResult = repo.getRecurring()
            val accountsResult = repo.getAccounts()
            if (rulesResult.isFailure || accountsResult.isFailure) {
                if (_state.value !is UiState.Success) {
                    _state.value = UiState.Error((rulesResult.exceptionOrNull() ?: accountsResult.exceptionOrNull())?.message ?: "Failed to load")
                }
                return@launch
            }
            _state.value = UiState.Success(RecurringScreenData(rulesResult.getOrDefault(emptyList()), accountsResult.getOrDefault(emptyList())))
        }
    }

    fun saveRule(
        id: Int?, name: String, type: String, accountId: Int, amount: Double, currency: String,
        frequency: String, nextDate: String, category: String, description: String, tags: List<String>, active: Boolean
    ) {
        viewModelScope.launch {
            val body = RecurringRequest(name, type, accountId, amount, currency, frequency, nextDate, category = category, description = description, tags = tags, active = active)
            val result = if (id == null) repo.createRecurring(body) else repo.updateRecurring(id, body)
            result.fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to save recurring rule" })
        }
    }

    fun deleteRule(id: Int) {
        viewModelScope.launch {
            repo.deleteRecurring(id).fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to delete rule" })
        }
    }

    fun postNow(id: Int) {
        viewModelScope.launch {
            repo.postRecurringNow(id).fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to post transaction" })
        }
    }

    fun clearActionError() { _actionError.value = null }
}
