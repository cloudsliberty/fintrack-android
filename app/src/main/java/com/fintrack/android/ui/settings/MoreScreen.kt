@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun MoreScreen(
    onOpenBudgets: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenTransfers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
            MoreItem("Budgets", Icons.Filled.PieChart, onOpenBudgets)
            HorizontalDivider()
            MoreItem("Categories & Tags", Icons.Filled.Category, onOpenCategories)
            HorizontalDivider()
            MoreItem("Recurring", Icons.Filled.Repeat, onOpenRecurring)
            HorizontalDivider()
            MoreItem("Transfers", Icons.Filled.SwapHoriz, onOpenTransfers)
            HorizontalDivider()
            MoreItem("Settings", Icons.Filled.Settings, onOpenSettings)
            HorizontalDivider()
            MoreItem("About", Icons.Filled.Info, onOpenAbout)
        }
    }
}

@Composable
private fun MoreItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
