package com.fintrack.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fintrack.android.data.SyncState
import com.fintrack.android.data.SyncStatusManager
import com.fintrack.android.data.sync.PendingSyncQueue
import com.fintrack.android.data.sync.SyncScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small "connecting to server…" / "data refreshing…" / "offline — showing cached data" / "N pending"
 * pill. Tapping it (when there's something queued) opens a sheet listing what's waiting to sync.
 */
@Composable
fun SyncStatusPill(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by SyncStatusManager.state.collectAsState()
    val pendingCount by SyncStatusManager.pendingCount.collectAsState()
    var showPendingSheet by remember { mutableStateOf(false) }

    val label = when {
        pendingCount > 0 && state !is SyncState.Refreshing && state !is SyncState.Connecting ->
            if (pendingCount == 1) "1 item pending sync" else "$pendingCount items pending sync"
        state is SyncState.Connecting -> "Connecting to server…"
        state is SyncState.Refreshing -> "Data refreshing…"
        state is SyncState.Offline -> (state as SyncState.Offline).message
        else -> null
    }
    val tappable = pendingCount > 0

    AnimatedVisibility(visible = label != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp,
            modifier = if (tappable) Modifier.clickable { showPendingSheet = true } else Modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                if (state is SyncState.Refreshing || state is SyncState.Connecting) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                    Spacer(Modifier.size(6.dp))
                }
                Text(label ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showPendingSheet) {
        val pending = remember(pendingCount) { PendingSyncQueue.all(context) }
        val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showPendingSheet = false },
            title = { Text("Waiting to sync") },
            text = {
                if (pending.isEmpty()) {
                    Text("Nothing queued right now.")
                } else {
                    LazyColumn {
                        items(pending, key = { it.id }) { op ->
                            Row(modifier = Modifier.padding(vertical = 6.dp)) {
                                androidx.compose.foundation.layout.Column {
                                    Text(op.summary, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        dateFormat.format(Date(op.createdAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    SyncScheduler.enqueueNow(context)
                    showPendingSheet = false
                }) { Text("Sync now") }
            },
            dismissButton = { TextButton(onClick = { showPendingSheet = false }) { Text("Close") } }
        )
    }
}
