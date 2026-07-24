package com.fintrack.android.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * A local, on-device PIN lock: separate from FinTrack's server-side "App Lock" password (which
 * gates specific write actions via the Nextcloud API). This one only ever touches this device —
 * it decides whether to show [PinUnlockScreen] before the rest of the app is reachable at all.
 *
 * - The PIN is always required on a cold app start (fresh process).
 * - After that, it's required again once the app has been in the background for longer than
 *   the configured timeout; a quick app-switch within the timeout doesn't re-prompt.
 */
object PinLockManager {
    private const val PREFS_NAME = "fintrack_pin_lock"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_TIMEOUT_MINUTES = "timeout_minutes"
    private const val KEY_BACKGROUNDED_AT = "backgrounded_at"
    private const val SALT = "fintrack-pin-v1" // not secret; just avoids trivially-identical hashes across installs

    // Cold-start default: nothing has unlocked this process yet, so the gate always shows first
    // if a PIN is configured — this flag flips true only once the correct PIN has been entered.
    private var hasUnlockedThisProcess = false

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest((SALT + pin).toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun timeoutMinutes(context: Context): Int = prefs(context).getInt(KEY_TIMEOUT_MINUTES, 5)

    /** Enables the lock (or updates the PIN/timeout of an already-enabled one). */
    fun setPin(context: Context, pin: String, timeoutMinutes: Int) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_PIN_HASH, hash(pin))
            .putInt(KEY_TIMEOUT_MINUTES, timeoutMinutes)
            .apply()
        hasUnlockedThisProcess = true // setting it up counts as being unlocked right now
    }

    /** Verifies [currentPin] and, if correct, disables the lock entirely. Returns whether it succeeded. */
    fun disable(context: Context, currentPin: String): Boolean {
        if (!verifyPin(context, currentPin)) return false
        prefs(context).edit().clear().apply()
        return true
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val stored = prefs(context).getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    /** Call from the app's lifecycle observer when it goes to the background. */
    fun recordBackgrounded(context: Context) {
        prefs(context).edit().putLong(KEY_BACKGROUNDED_AT, System.currentTimeMillis()).apply()
    }

    /** Call once the person has entered the correct PIN (or right after setting one up). */
    fun markUnlocked(context: Context) {
        hasUnlockedThisProcess = true
        prefs(context).edit().remove(KEY_BACKGROUNDED_AT).apply()
    }

    /** Whether [PinUnlockScreen] should be shown right now, given the enabled/timeout state. */
    fun isLockRequiredNow(context: Context): Boolean {
        if (!isEnabled(context)) return false
        if (!hasUnlockedThisProcess) return true
        val backgroundedAt = prefs(context).getLong(KEY_BACKGROUNDED_AT, -1L)
        if (backgroundedAt < 0) return false
        val timeoutMs = timeoutMinutes(context) * 60_000L
        return System.currentTimeMillis() - backgroundedAt >= timeoutMs
    }

    /** Full reset (used by the "forgot PIN" escape hatch, alongside a full logout). */
    fun reset(context: Context) {
        hasUnlockedThisProcess = false
        prefs(context).edit().clear().apply()
    }
}
