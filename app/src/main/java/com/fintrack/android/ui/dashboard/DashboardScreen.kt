@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.RecurringRule
import com.fintrack.android.ui.common.*
import com.fintrack.android.ui.theme.FtExpense
import com.fintrack.android.ui.theme.FtIncome

@Composable
fun DashboardScreen() {
    val viewModel: DashboardViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Dashboard") }) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> DashboardContent(s.data)
            }
        }
    }
}

@Composable
private fun DashboardContent(data: DashboardData) {
    val base = data.summary.baseCurrency
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(180.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { StatCard("Net Worth", formatMoney(data.netWorth, base)) }
                item { StatCard("Cash Flow", formatMoney(data.cashFlow, base), valueColor = if (data.cashFlow >= 0) FtIncome else FtExpense) }
                item { StatCard("Total Assets", formatMoney(data.totalAssets, base)) }
                item { StatCard("Total Liabilities", formatMoney(data.totalLiabilities, base)) }
            }
        }

        if (data.activeBudgets.isNotEmpty()) {
            item { Text("Budgets vs. Spending", style = MaterialTheme.typography.titleMedium) }
            items(data.activeBudgets, key = { "budget-${it.budget.id}" }) { budget -> BudgetSummaryRow(budget) }
        }

        if (data.topExpenseCategories.isNotEmpty()) {
            item { Text("Top Spending Categories", style = MaterialTheme.typography.titleMedium) }
            items(data.topExpenseCategories, key = { "cat-${it.category}" }) { spend ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(spend.category)
                    Text(formatMoney(spend.total, base), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { Text("Upcoming Recurring Transactions", style = MaterialTheme.typography.titleMedium) }
        if (data.upcomingRecurring.isEmpty()) {
            item { Text("No upcoming recurring transactions.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(data.upcomingRecurring, key = { "rec-${it.id}" }) { rule -> UpcomingRecurringRow(rule) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun BudgetSummaryRow(progress: BudgetProgress) {
    val budget = progress.budget
    val ratio = if (budget.limitAmount > 0) (progress.spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(budget.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatMoney(progress.spent, budget.currency)} / ${formatMoney(budget.limitAmount, budget.currency)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth(),
                color = if (ratio >= 1f) FtExpense else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun UpcomingRecurringRow(rule: RecurringRule) {
    val isIncome = rule.type == "income"
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name.ifBlank { rule.description.ifBlank { "Untitled" } }, fontWeight = FontWeight.SemiBold)
                Text(
                    "Due ${rule.nextDate} · ${rule.frequency.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (if (isIncome) "+" else "-") + formatMoney(rule.amount, rule.currency),
                color = if (isIncome) FtIncome else FtExpense,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
