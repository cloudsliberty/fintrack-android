package com.fintrack.android

import android.app.Application
import com.fintrack.android.data.SyncStatusManager
import com.fintrack.android.data.sync.PendingSyncQueue
import com.fintrack.android.data.sync.SyncScheduler

class FinTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncStatusManager.setPendingCount(PendingSyncQueue.count(this))
        SyncScheduler.startWatchingConnectivity(this)
        // Also try once at cold start in case connectivity was already up before the callback fired.
        if (PendingSyncQueue.count(this) > 0) {
            SyncScheduler.enqueueNow(this)
        }
    }
}
