package com.fintrack.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.android.data.SessionManager
import com.fintrack.android.data.model.*
import com.fintrack.android.data.network.ApiClient
import com.fintrack.android.data.repository.FinTrackRepository
import com.fintrack.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsScreenData(
    val summary: Summary,
    val currencies: List<Currency>,
    val lockStatus: LockStatus,
    val loginName: String,
    val serverUrl: String
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<SettingsScreenData>>(UiState.Loading)
    val state: StateFlow<UiState<SettingsScreenData>> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val summaryResult = repo.getSummary()
            val currenciesResult = repo.getCurrencies()
            val lockResult = repo.getLockStatus()
            val session = SessionManager.current(getApplication())

            val error = listOf(summaryResult, currenciesResult, lockResult).firstOrNull { it.isFailure }
            if (error != null || session == null) {
                _state.value = UiState.Error(error?.exceptionOrNull()?.message ?: "Failed to load settings")
                return@launch
            }
            _state.value = UiState.Success(
                SettingsScreenData(
                    summary = summaryResult.getOrDefault(Summary()),
                    currencies = currenciesResult.getOrDefault(emptyList()),
                    lockStatus = lockResult.getOrDefault(LockStatus()),
                    loginName = session.loginName,
                    serverUrl = session.serverUrl
                )
            )
        }
    }

    fun saveCurrency(id: Int?, code: String, name: String, symbol: String, rate: Double) {
        viewModelScope.launch {
            val body = CurrencyRequest(code, name, symbol, rate)
            val result = if (id == null) repo.createCurrency(body) else repo.updateCurrency(id, body)
            result.fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to save currency" })
        }
    }

    fun deleteCurrency(id: Int) {
        viewModelScope.launch {
            repo.deleteCurrency(id).fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to delete currency" })
        }
    }

    fun setupLock(password: String, timeoutMinutes: Int) {
        viewModelScope.launch {
            repo.setupLock(LockSetupRequest(newPassword = password, timeoutMinutes = timeoutMinutes)).fold(
                onSuccess = { load(); _actionMessage.value = "App lock enabled" },
                onFailure = { _actionError.value = it.message ?: "Failed to set up app lock" }
            )
        }
    }

    fun disableLock(currentPassword: String) {
        viewModelScope.launch {
            repo.disableLock(LockDisableRequest(currentPassword)).fold(
                onSuccess = { load(); _actionMessage.value = "App lock disabled" },
                onFailure = { _actionError.value = it.message ?: "Failed to disable app lock" }
            )
        }
    }

    fun logout() {
        SessionManager.logout(getApplication())
        ApiClient.invalidate()
    }

    fun clearActionError() { _actionError.value = null }
    fun clearActionMessage() { _actionMessage.value = null }
}
