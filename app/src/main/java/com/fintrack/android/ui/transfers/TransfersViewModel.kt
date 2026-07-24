package com.fintrack.android.ui.transfers

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

data class TransfersScreenData(val transfers: List<Transfer>, val accounts: List<Account>)

class TransfersViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<TransfersScreenData>>(UiState.Loading)
    val state: StateFlow<UiState<TransfersScreenData>> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init { load() }

    fun load() {
        val cachedTransfers = repo.peekCache<List<Transfer>>(FinTrackRepository.KEY_TRANSFERS, object : com.google.gson.reflect.TypeToken<List<Transfer>>() {}.type)
        val cachedAccounts = repo.peekCache<List<Account>>(FinTrackRepository.KEY_ACCOUNTS, repo.accountsType)
        if (cachedTransfers != null || cachedAccounts != null) {
            _state.value = UiState.Success(TransfersScreenData(cachedTransfers ?: emptyList(), cachedAccounts ?: emptyList()))
        } else if (_state.value !is UiState.Success) {
            _state.value = UiState.Loading
        }
        viewModelScope.launch {
            val transfersResult = repo.getTransfers()
            val accountsResult = repo.getAccounts()
            if (transfersResult.isFailure || accountsResult.isFailure) {
                if (_state.value !is UiState.Success) {
                    _state.value = UiState.Error((transfersResult.exceptionOrNull() ?: accountsResult.exceptionOrNull())?.message ?: "Failed to load")
                }
                return@launch
            }
            _state.value = UiState.Success(TransfersScreenData(transfersResult.getOrDefault(emptyList()), accountsResult.getOrDefault(emptyList())))
        }
    }

    fun createTransfer(fromAccountId: Int, toAccountId: Int, fromAmount: Double, toAmount: Double, fromCurrency: String, toCurrency: String, conversionRate: Double, description: String, date: String) {
        viewModelScope.launch {
            val body = TransferRequest(fromAccountId, toAccountId, fromAmount, toAmount, fromCurrency, toCurrency, conversionRate, description, date)
            repo.createTransfer(body).fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to create transfer" })
        }
    }

    fun deleteTransfer(id: Int) {
        viewModelScope.launch {
            repo.deleteTransfer(id).fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to delete transfer" })
        }
    }

    fun clearActionError() { _actionError.value = null }
}
