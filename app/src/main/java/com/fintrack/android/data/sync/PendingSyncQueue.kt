package com.fintrack.android.data.sync

import android.content.Context
import com.fintrack.android.data.OfflineCache
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Durable queue of writes made while offline. Backed by the same SharedPreferences-JSON store as
 * [OfflineCache] (simple, no schema migrations to manage) rather than a full Room table, since this
 * is a short-lived retry queue, not permanent app data.
 */
object PendingSyncQueue {
    private const val KEY = "pending_sync_queue"
    private val TYPE = object : TypeToken<List<PendingOperation>>() {}.type

    @Synchronized
    fun all(context: Context): List<PendingOperation> = OfflineCache.get(context, KEY, TYPE) ?: emptyList()

    @Synchronized
    fun enqueue(context: Context, type: PendingOpType, payloadJson: String, targetId: Int?, localId: Int?, summary: String): PendingOperation {
        val op = PendingOperation(
            id = UUID.randomUUID().toString(),
            type = type,
            payloadJson = payloadJson,
            targetId = targetId,
            localId = localId,
            summary = summary,
            createdAt = System.currentTimeMillis()
        )
        OfflineCache.put(context, KEY, all(context) + op)
        return op
    }

    @Synchronized
    fun remove(context: Context, opId: String) {
        OfflineCache.put(context, KEY, all(context).filterNot { it.id == opId })
    }

    fun count(context: Context): Int = all(context).size
}
