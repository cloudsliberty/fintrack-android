package com.fintrack.android.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class SyncState {
    data object Idle : SyncState()
    data object Connecting : SyncState()
    data object Refreshing : SyncState()
    data class Offline(val message: String = "Offline — showing cached data") : SyncState()
}

/**
 * App-wide sync status so any screen (via the shared top bar in FinTrackNavGraph) can show a small
 * "connecting to server…" / "data refreshing…" / "offline — showing cached data" indicator without
 * every ViewModel having to independently wire up its own status UI. Also tracks how many writes
 * are queued for the background sync worker, for the "N pending" tappable chip.
 */
object SyncStatusManager {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private var inFlight = 0

    @Synchronized
    fun begin() {
        inFlight++
        if (_state.value !is SyncState.Offline) _state.value = SyncState.Refreshing
    }

    @Synchronized
    fun end(usedCacheFallback: Boolean, message: String? = null) {
        inFlight = (inFlight - 1).coerceAtLeast(0)
        if (inFlight == 0) {
            _state.value = if (usedCacheFallback) SyncState.Offline(message ?: "Offline — showing cached data") else SyncState.Idle
        }
    }

    fun connecting() { _state.value = SyncState.Connecting }

    fun setPendingCount(count: Int) { _pendingCount.value = count }
}
