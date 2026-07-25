@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.fintrack.android.BuildConfig
import com.fintrack.android.R
import com.fintrack.android.data.SessionManager
import com.fintrack.android.data.security.BiometricAuthHelper
import com.fintrack.android.data.security.PinLockManager

@Composable
fun PinUnlockScreen(onUnlocked: () -> Unit, onForgotPinLogout: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showForgotConfirm by remember { mutableStateOf(false) }
    val biometricAvailable = remember { PinLockManager.isBiometricEnabled(context) && BiometricAuthHelper.isAvailable(context) }

    fun unlock() {
        PinLockManager.markUnlocked(context)
        onUnlocked()
    }

    fun tryPinUnlock() {
        if (PinLockManager.verifyPin(context, pin)) {
            error = false
            unlock()
        } else {
            error = true
            pin = ""
        }
    }

    fun promptBiometric() {
        val act = activity ?: return
        BiometricAuthHelper.authenticate(
            activity = act,
            onSuccess = { unlock() },
            onError = { /* fall back silently to PIN entry, already on screen */ },
            onUsePinInstead = { /* no-op — PIN field is always right there */ }
        )
    }

    // Offer biometric immediately when the lock screen appears, so it's a true shortcut rather
    // than an extra tap — the PIN field underneath is always available as a fallback.
    LaunchedEffect(Unit) {
        if (biometricAvailable) promptBiometric()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ft_logo),
                contentDescription = "FinTrack",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("FinTrack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))

            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Enter your PIN", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))

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
            Button(onClick = ::tryPinUnlock, enabled = pin.length in 4..6, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Unlock")
            }

            if (biometricAvailable) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = { promptBiometric() }, modifier = Modifier.fillMaxWidth(0.7f)) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use fingerprint")
                }
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
