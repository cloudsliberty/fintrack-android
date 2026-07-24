package com.fintrack.android.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.android.data.SessionManager
import com.fintrack.android.data.network.ApiClient
import com.fintrack.android.data.network.LoginFlowInit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    data object EnteringServer : LoginState()
    data object CheckingServer : LoginState()
    data class WaitingForBrowser(val loginUrl: String) : LoginState()
    data object LoggedIn : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<LoginState>(LoginState.EnteringServer)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var pollJob: Job? = null

    /** Normalizes what someone might type ("cloud.example.com", "example.com/nextcloud/") into a full https:// URL. */
    private fun normalizeServerUrl(input: String): String {
        var url = input.trim().trimEnd('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    fun startLogin(serverInput: String) {
        if (serverInput.isBlank()) {
            _state.value = LoginState.Error("Enter your Nextcloud server address")
            return
        }
        val serverUrl = normalizeServerUrl(serverInput)
        _state.value = LoginState.CheckingServer

        viewModelScope.launch {
            try {
                val init: LoginFlowInit = ApiClient.nextcloudAuth().initLoginFlow("$serverUrl/index.php/login/v2")
                _state.value = LoginState.WaitingForBrowser(init.login)
                startPolling(serverUrl, init.poll.endpoint, init.poll.token)
            } catch (e: Exception) {
                _state.value = LoginState.Error(
                    "Couldn't reach that server. Check the address and that it's a Nextcloud instance " +
                        "with FinTrack installed. (${e.message ?: "unknown error"})"
                )
            }
        }
    }

    private fun startPolling(fallbackServerUrl: String, endpoint: String, token: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val deadline = System.currentTimeMillis() + 15 * 60 * 1000L // Login Flow v2 tokens expire after 15 minutes
            while (System.currentTimeMillis() < deadline) {
                delay(1500)
                try {
                    val response = ApiClient.nextcloudAuth().pollLoginFlow(endpoint, token)
                    if (response.isSuccessful) {
                        val creds = response.body() ?: continue
                        // Nextcloud's own `server` field is authoritative (handles
                        // redirects/trailing paths); fall back to what was typed
                        // if it's ever missing.
                        val server = creds.server.trimEnd('/').ifBlank { fallbackServerUrl }
                        SessionManager.save(getApplication(), server, creds.loginName, creds.appPassword)
                        ApiClient.invalidate()
                        _state.value = LoginState.LoggedIn
                        return@launch
                    }
                    // 404 while waiting for approval is expected — keep polling.
                } catch (e: Exception) {
                    // Transient network hiccups shouldn't abort a 15-minute flow — keep polling.
                }
            }
            _state.value = LoginState.Error("Login timed out. Please try again.")
        }
    }

    fun cancelLogin() {
        pollJob?.cancel()
        _state.value = LoginState.EnteringServer
    }

    fun dismissError() {
        _state.value = LoginState.EnteringServer
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
