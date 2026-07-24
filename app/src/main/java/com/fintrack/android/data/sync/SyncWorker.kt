package com.fintrack.android.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fintrack.android.data.OfflineCache
import com.fintrack.android.data.SyncStatusManager
import com.fintrack.android.data.model.AccountRequest
import com.fintrack.android.data.model.TransactionRequest
import com.fintrack.android.data.repository.FinTrackRepository
import com.google.gson.Gson

/**
 * Drains [PendingSyncQueue]: replays every queued write against the real API in order, in the
 * order it was created. A queue item is only removed once the server confirms it — if the app is
 * still offline (or the server errors) it's left in place for the next run.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        val repo = FinTrackRepository(applicationContext)
        val pending = PendingSyncQueue.all(applicationContext)
        if (pending.isEmpty()) {
            SyncStatusManager.setPendingCount(0)
            return Result.success()
        }

        var allSucceeded = true
        for (op in pending) {
            val ok = replay(repo, op)
            if (ok) {
                PendingSyncQueue.remove(applicationContext, op.id)
            } else {
                allSucceeded = false
            }
        }

        SyncStatusManager.setPendingCount(PendingSyncQueue.count(applicationContext))
        // Bust the read caches so the next screen load pulls the server's authoritative copies
        // (with real ids) instead of our optimistic local placeholders.
        if (allSucceeded) {
            OfflineCache.clear(applicationContext)
        }
        return if (allSucceeded) Result.success() else Result.retry()
    }

    private suspend fun replay(repo: FinTrackRepository, op: PendingOperation): Boolean = when (op.type) {
        PendingOpType.CREATE_ACCOUNT -> {
            val body = gson.fromJson(op.payloadJson, AccountRequest::class.java)
            repo.createAccount(body).isSuccess
        }
        PendingOpType.UPDATE_ACCOUNT -> {
            val body = gson.fromJson(op.payloadJson, AccountRequest::class.java)
            op.targetId?.let { repo.updateAccount(it, body).isSuccess } ?: true
        }
        PendingOpType.CREATE_TRANSACTION -> {
            val body = gson.fromJson(op.payloadJson, TransactionRequest::class.java)
            repo.createTransaction(body).isSuccess
        }
        PendingOpType.UPDATE_TRANSACTION -> {
            val body = gson.fromJson(op.payloadJson, TransactionRequest::class.java)
            op.targetId?.let { repo.updateTransaction(it, body).isSuccess } ?: true
        }
    }
}
