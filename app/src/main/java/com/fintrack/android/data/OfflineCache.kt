package com.fintrack.android.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Every list-shaped GET response (accounts, transactions, categories, tags, currencies, budgets,
 * recurring rules, transfers, summary) is mirrored here as raw JSON the moment it's fetched
 * successfully. If a later network call fails — no connection, server unreachable — the repository
 * falls back to whatever was last cached here instead of leaving the screen blank, so the app stays
 * usable offline. Deliberately a plain SharedPreferences blob store (not a full Room DB): this is a
 * "last known good" cache for read-through fallback, not a source of truth the app writes through.
 */
object OfflineCache {
    private const val PREFS_NAME = "fintrack_offline_cache"
    private val gson = Gson()

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun <T> put(context: Context, key: String, value: T) {
        prefs(context).edit().putString(key, gson.toJson(value)).apply()
    }

    fun <T> get(context: Context, key: String, type: Type): T? {
        val json = prefs(context).getString(key, null) ?: return null
        return try {
            gson.fromJson<T>(json, type)
        } catch (e: Exception) {
            null
        }
    }

    inline fun <reified T> get(context: Context, key: String): T? = get(context, key, object : TypeToken<T>() {}.type)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** All stored keys starting with [prefix] — used to patch every cached filter-variant of a list (e.g. all "transactions:*" entries) after an offline write. */
    fun keysWithPrefix(context: Context, prefix: String): List<String> =
        prefs(context).all.keys.filter { it.startsWith(prefix) }
}
