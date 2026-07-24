@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    var serverInput by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is LoginState.LoggedIn) onLoggedIn()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("FinTrack", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Sign in with your Nextcloud account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            when (val s = state) {
                is LoginState.EnteringServer, is LoginState.Error -> {
                    OutlinedTextField(
                        value = serverInput,
                        onValueChange = { serverInput = it },
                        label = { Text("Nextcloud server address") },
                        placeholder = { Text("cloud.example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.startLogin(serverInput) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue") }

                    if (s is LoginState.Error) {
                        Spacer(Modifier.height(16.dp))
                        Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                is LoginState.CheckingServer -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                is LoginState.WaitingForBrowser -> {
                    LaunchedEffect(s.loginUrl) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.loginUrl)))
                    }
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Finish signing in in your browser, then come back here.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.loginUrl))) }) {
                        Text("Reopen browser")
                    }
                    TextButton(onClick = { viewModel.cancelLogin() }) { Text("Cancel") }
                }

                is LoginState.LoggedIn -> { /* handled by LaunchedEffect above */ }
            }
        }
    }
}
