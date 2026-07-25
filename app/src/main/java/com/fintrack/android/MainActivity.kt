package com.fintrack.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fintrack.android.data.security.PinLockManager
import com.fintrack.android.ui.navigation.FinTrackNavGraph
import com.fintrack.android.ui.security.PinUnlockScreen
import com.fintrack.android.ui.theme.FinTrackTheme

// FragmentActivity (not plain ComponentActivity) because androidx.biometric's BiometricPrompt
// needs a FragmentManager to host its system dialog — it's still fully Compose-compatible,
// setContent {} works exactly the same.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinTrackRoot()
        }
    }
}

@Composable
private fun FinTrackRoot() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Cold start (or process restart) always begins locked if a PIN is configured; a background
    // pause shorter than the configured timeout won't re-prompt (see PinLockManager).
    var locked by remember { mutableStateOf(PinLockManager.isLockRequiredNow(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> PinLockManager.recordBackgrounded(context)
                Lifecycle.Event.ON_START -> {
                    if (PinLockManager.isLockRequiredNow(context)) locked = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    FinTrackTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (locked) {
                PinUnlockScreen(
                    onUnlocked = { locked = false },
                    onForgotPinLogout = { locked = false }
                )
            } else {
                FinTrackNavGraph()
            }
        }
    }
}
