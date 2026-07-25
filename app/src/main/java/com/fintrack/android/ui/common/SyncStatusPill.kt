package com.fintrack.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fintrack.android.data.SyncState
import com.fintrack.android.data.SyncStatusManager
import com.fintrack.android.data.sync.PendingSyncQueue
import com.fintrack.android.data.sync.SyncScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SyncGreen = Color(0xFF2E7D32)
private val SyncRed = Color(0xFFD32F2F)

/**
 * Icon-only sync status indicator, top-right: a green cloud when the last refresh succeeded, a
 * red cloud when offline (showing cached data), or a small spinner while a fetch is in flight.
 * A numeric badge appears on top when there are queued offline writes — tapping it (only when
 * something's queued) opens a sheet listing what's waiting to sync.
 */
@Composable
fun SyncStatusPill(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by SyncStatusManager.state.collectAsState()
    val pendingCount by SyncStatusManager.pendingCount.collectAsState()
    var showPendingSheet by remember { mutableStateOf(false) }
    val tappable = pendingCount > 0

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp,
        modifier = modifier.then(if (tappable) Modifier.clickable { showPendingSheet = true } else Modifier)
    ) {
        Box(modifier = Modifier.padding(7.dp), contentAlignment = Alignment.Center) {
            BadgedBox(badge = {
                if (pendingCount > 0) {
                    Badge { Text(pendingCount.toString()) }
                }
            }) {
                when (state) {
                    is SyncState.Connecting, is SyncState.Refreshing ->
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    is SyncState.Offline ->
                        Icon(Icons.Filled.CloudOff, contentDescription = "Offline — showing cached data", tint = SyncRed, modifier = Modifier.size(20.dp))
                    is SyncState.Idle ->
                        Icon(Icons.Filled.CloudDone, contentDescription = "Up to date", tint = SyncGreen, modifier = Modifier.size(20.dp))
                }
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
                                Column {
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
