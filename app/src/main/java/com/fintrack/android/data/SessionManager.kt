package com.fintrack.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds the three pieces of state that make up a logged-in session:
 * the Nextcloud server URL, the login name, and the app password obtained
 * via Login Flow v2 (see NextcloudAuthApi). Stored in EncryptedSharedPreferences
 * so the app password — which grants full API access to the account's
 * FinTrack data — never sits on disk in plain text.
 *
 * This is intentionally a plain singleton rather than a DataStore/Flow-based
 * reactive store: credentials only change on login/logout, and every screen
 * that needs them reads them once at repository-construction time.
 */
object SessionManager {

    private const val PREFS_NAME = "fintrack_session"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_LOGIN_NAME = "login_name"
    private const val KEY_APP_PASSWORD = "app_password"

    private var prefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        return prefs ?: run {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { prefs = it }
        }
    }

    data class Session(val serverUrl: String, val loginName: String, val appPassword: String)

    fun save(context: Context, serverUrl: String, loginName: String, appPassword: String) {
        prefs(context).edit()
            .putString(KEY_SERVER_URL, serverUrl.trimEnd('/'))
            .putString(KEY_LOGIN_NAME, loginName)
            .putString(KEY_APP_PASSWORD, appPassword)
            .apply()
    }

    fun current(context: Context): Session? {
        val p = prefs(context)
        val server = p.getString(KEY_SERVER_URL, null) ?: return null
        val login = p.getString(KEY_LOGIN_NAME, null) ?: return null
        val password = p.getString(KEY_APP_PASSWORD, null) ?: return null
        return Session(server, login, password)
    }

    fun isLoggedIn(context: Context): Boolean = current(context) != null

    fun logout(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** Base URL for FinTrack's own API, built from the stored server URL. */
    fun apiBaseUrl(context: Context): String? {
        val session = current(context) ?: return null
        return "${session.serverUrl}/index.php/apps/fintrack/api/"
    }
}
