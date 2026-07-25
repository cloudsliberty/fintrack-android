package com.fintrack.android.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Fingerprint/face unlock is only ever a *shortcut* to the PIN — it's never the sole lock
 * mechanism (the PIN remains the fallback if biometrics fail, are removed from the device, or
 * the person just prefers it), which is why this has no concept of "set up biometrics" on its
 * own; it just asks the OS whether the device can currently authenticate.
 */
object BiometricAuthHelper {

    private const val ALLOWED = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUsePinInstead: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // The person tapped the prompt's own "Use PIN instead" / cancel button.
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    onUsePinInstead()
                } else {
                    onError(errString.toString())
                }
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinTrack")
            .setSubtitle("Use your fingerprint or face to continue")
            .setNegativeButtonText("Use PIN instead")
            .setAllowedAuthenticators(ALLOWED)
            .build()

        prompt.authenticate(info)
    }
}
