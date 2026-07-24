package com.fintrack.android.data.sync

enum class PendingOpType { CREATE_ACCOUNT, UPDATE_ACCOUNT, CREATE_TRANSACTION, UPDATE_TRANSACTION }

/**
 * One write that couldn't reach the server (device was offline) and is queued to retry.
 * [payloadJson] holds the AccountRequest/TransactionRequest body, [targetId] the id being
 * updated (null for creates). [localId] is the negative placeholder id the optimistic local
 * copy was given so the UI has something to show immediately.
 */
data class PendingOperation(
    val id: String,
    val type: PendingOpType,
    val payloadJson: String,
    val targetId: Int? = null,
    val localId: Int? = null,
    val summary: String,
    val createdAt: Long
)
