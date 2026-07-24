@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.Currency
import com.fintrack.android.data.security.PinLockManager
import com.fintrack.android.ui.common.*

@Composable
fun SettingsScreen(onLoggedOut: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    var showAddCurrency by remember { mutableStateOf(false) }
    var pendingDeleteCurrency by remember { mutableStateOf<Currency?>(null) }
    var showLockSetup by remember { mutableStateOf(false) }
    var showLockDisable by remember { mutableStateOf(false) }
    var pendingLogout by remember { mutableStateOf(false) }

    // Local, on-device PIN lock (separate from the server-tracked "App Lock" above) — its state
    // isn't a StateFlow, so we bump this counter to force a re-read after any change.
    val context = androidx.compose.ui.platform.LocalContext.current
    var pinLockVersion by remember { mutableStateOf(0) }
    val pinEnabled = remember(pinLockVersion) { PinLockManager.isEnabled(context) }
    val pinTimeout = remember(pinLockVersion) { PinLockManager.timeoutMinutes(context) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinDisable by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = s.data
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            SectionCard("Account") {
                                Text(data.loginName, fontWeight = FontWeight.SemiBold)
                                Text(data.serverUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item {
                            SectionCard("Overview") {
                                Text("Base currency: ${data.summary.baseCurrency}")
                                Text("${data.summary.totalAccounts} accounts · ${data.summary.totalTransactions} transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item {
                            SectionCard(
                                title = "Currencies",
                                action = { TextButton(onClick = { showAddCurrency = true }) { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)); Text(" Add") } }
                            ) {
                                if (data.currencies.isEmpty()) {
                                    Text("No extra currencies configured.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    data.currencies.forEach { currency ->
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${currency.code} — ${currency.name}")
                                                Text("1 ${data.summary.baseCurrency} = ${currency.rate} ${currency.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = { pendingDeleteCurrency = currency }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SectionCard("App Lock") {
                                if (data.lockStatus.enabled) {
                                    Text("Enabled — locks after ${data.lockStatus.timeoutMinutes} minutes idle.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = { showLockDisable = true }) { Text("Disable App Lock") }
                                } else {
                                    Text("Adds a PIN on top of your Nextcloud login for this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { showLockSetup = true }) { Text("Enable App Lock") }
                                }
                            }
                        }
                        item {
                            SectionCard("PIN Lock") {
                                if (pinEnabled) {
                                    Text(
                                        "Enabled — asks for your PIN every time you open FinTrack, and again if it's been in the background more than $pinTimeout minutes.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { showPinSetup = true }) { Text("Change PIN") }
                                        OutlinedButton(onClick = { showPinDisable = true }) { Text("Disable") }
                                    }
                                } else {
                                    Text(
                                        "A quick on-device PIN, separate from your Nextcloud login — asked every time the app opens.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { showPinSetup = true }) { Text("Enable PIN Lock") }
                                }
                            }
                        }
                        item {
                            Button(onClick = { pendingLogout = true }, modifier = Modifier.fillMaxWidth()) { Text("Log Out") }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (showAddCurrency) {
        CurrencyEditorDialog(onDismiss = { showAddCurrency = false }, onSave = { code, name, symbol, rate ->
            viewModel.saveCurrency(null, code, name, symbol, rate); showAddCurrency = false
        })
    }
    pendingDeleteCurrency?.let { currency ->
        ConfirmDialog(title = "Delete currency?", message = "\"${currency.code}\" will be removed.", onConfirm = { viewModel.deleteCurrency(currency.id) }, onDismiss = { pendingDeleteCurrency = null })
    }
    if (showLockSetup) {
        LockSetupDialog(onDismiss = { showLockSetup = false }, onConfirm = { password, timeout ->
            viewModel.setupLock(password, timeout); showLockSetup = false
        })
    }
    if (showLockDisable) {
        PasswordPromptDialog(title = "Disable App Lock", onDismiss = { showLockDisable = false }, onConfirm = { pw ->
            viewModel.disableLock(pw); showLockDisable = false
        })
    }
    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onConfirm = { pin, timeout ->
                PinLockManager.setPin(context, pin, timeout)
                pinLockVersion++
                showPinSetup = false
            }
        )
    }
    if (showPinDisable) {
        PinPromptDialog(
            title = "Disable PIN Lock",
            onDismiss = { showPinDisable = false },
            onConfirm = { pin ->
                val ok = PinLockManager.disable(context, pin)
                if (ok) { pinLockVersion++; showPinDisable = false }
                ok
            }
        )
    }
    if (pendingLogout) {
        ConfirmDialog(
            title = "Log out?", message = "You'll need to sign in again to access your FinTrack data.",
            confirmLabel = "Log Out", onConfirm = { viewModel.logout(); onLoggedOut() }, onDismiss = { pendingLogout = false }
        )
    }
    actionError?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearActionError, title = { Text("Error") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearActionError) { Text("OK") } })
    }
    actionMessage?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearActionMessage, title = { Text("Done") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearActionMessage) { Text("OK") } })
    }
}

@Composable
private fun SectionCard(title: String, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun CurrencyEditorDialog(onDismiss: () -> Unit, onSave: (code: String, name: String, symbol: String, rate: Double) -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("1.0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Currency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase().take(3) }, label = { Text("Code (e.g. EUR)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rateText, onValueChange = { rateText = it }, label = { Text("Rate (1 base = ? this currency)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = code.length == 3 && rateText.toDoubleOrNull() != null, onClick = { onSave(code, name.trim(), symbol.trim(), rateText.toDoubleOrNull() ?: 1.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LockSetupDialog(onDismiss: () -> Unit, onConfirm: (password: String, timeoutMinutes: Int) -> Unit) {
    var password by remember { mutableStateOf("") }
    var timeoutText by remember { mutableStateOf("10") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable App Lock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("PIN / passphrase") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = timeoutText, onValueChange = { timeoutText = it }, label = { Text("Lock after (minutes idle)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = password.length >= 4, onClick = { onConfirm(password, timeoutText.toIntOrNull() ?: 10) }) { Text("Enable") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PasswordPromptDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Current PIN / passphrase") }, singleLine = true) },
        confirmButton = { TextButton(enabled = password.isNotBlank(), onClick = { onConfirm(password) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (pin: String, timeoutMinutes: Int) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val timeoutOptions = listOf(0 to "Immediately", 1 to "1 minute", 5 to "5 minutes", 15 to "15 minutes", 30 to "30 minutes")
    var timeoutMinutes by remember { mutableStateOf(5) }
    var timeoutMenuOpen by remember { mutableStateOf(false) }
    val mismatch = confirmPin.isNotEmpty() && pin != confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Up PIN Lock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose a 4–6 digit PIN. You'll be asked for it every time you open FinTrack.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("New PIN") }, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("Confirm PIN") }, singleLine = true, isError = mismatch,
                    supportingText = if (mismatch) { { Text("PINs don't match") } } else null,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = timeoutMenuOpen, onExpandedChange = { timeoutMenuOpen = it }) {
                    OutlinedTextField(
                        value = timeoutOptions.first { it.first == timeoutMinutes }.second, onValueChange = {}, readOnly = true,
                        label = { Text("Re-lock after (background timeout)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeoutMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = timeoutMenuOpen, onDismissRequest = { timeoutMenuOpen = false }) {
                        timeoutOptions.forEach { (minutes, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { timeoutMinutes = minutes; timeoutMenuOpen = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.length in 4..6 && pin == confirmPin,
                onClick = { onConfirm(pin, timeoutMinutes) }
            ) { Text("Enable") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PinPromptDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; error = false } },
                label = { Text("Current PIN") }, singleLine = true, isError = error,
                supportingText = if (error) { { Text("Incorrect PIN") } } else null,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
            )
        },
        confirmButton = {
            TextButton(enabled = pin.isNotBlank(), onClick = { if (!onConfirm(pin)) { error = true; pin = "" } }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
