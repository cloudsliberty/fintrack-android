@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fintrack.android.data.SessionManager
import com.fintrack.android.data.security.PinLockManager

@Composable
fun PinUnlockScreen(onUnlocked: () -> Unit, onForgotPinLogout: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showForgotConfirm by remember { mutableStateOf(false) }

    fun tryUnlock() {
        if (PinLockManager.verifyPin(context, pin)) {
            error = false
            PinLockManager.markUnlocked(context)
            onUnlocked()
        } else {
            error = true
            pin = ""
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Enter your PIN", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; error = false } },
                label = { Text("PIN") },
                singleLine = true,
                isError = error,
                supportingText = if (error) { { Text("Incorrect PIN") } } else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = ::tryUnlock, enabled = pin.length in 4..6, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Unlock")
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { showForgotConfirm = true }) { Text("Forgot PIN? Log out") }
        }
    }

    if (showForgotConfirm) {
        AlertDialog(
            onDismissRequest = { showForgotConfirm = false },
            title = { Text("Log out?") },
            text = { Text("This clears the PIN lock and signs you out of FinTrack on this device. You'll need to log in to your Nextcloud server again.") },
            confirmButton = {
                TextButton(onClick = {
                    PinLockManager.reset(context)
                    SessionManager.logout(context)
                    showForgotConfirm = false
                    onForgotPinLogout()
                }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { showForgotConfirm = false }) { Text("Cancel") } }
        )
    }
}
