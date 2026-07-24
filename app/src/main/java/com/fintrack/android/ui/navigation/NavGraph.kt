@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fintrack.android.data.SessionManager
import com.fintrack.android.ui.accounts.AccountsScreen
import com.fintrack.android.ui.budgets.BudgetsScreen
import com.fintrack.android.ui.categories.CategoriesScreen
import com.fintrack.android.ui.common.SyncStatusPill
import com.fintrack.android.ui.dashboard.DashboardScreen
import com.fintrack.android.ui.login.LoginScreen
import com.fintrack.android.ui.recurring.RecurringScreen
import com.fintrack.android.ui.settings.AboutScreen
import com.fintrack.android.ui.settings.MoreScreen
import com.fintrack.android.ui.settings.SettingsScreen
import com.fintrack.android.ui.transactions.TransactionsScreen
import com.fintrack.android.ui.transfers.TransfersScreen

private object Routes {
    const val LOGIN = "login"
    const val TRANSACTIONS = "transactions"
    const val ACCOUNTS = "accounts"
    const val DASHBOARD = "dashboard"
    const val MORE = "more"
    const val BUDGETS = "budgets"
    const val CATEGORIES = "categories"
    const val RECURRING = "recurring"
    const val TRANSFERS = "transfers"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private const val ACCOUNT_ID_ARG = "accountId"
private const val TRANSACTIONS_ROUTE = "${Routes.TRANSACTIONS}?$ACCOUNT_ID_ARG={$ACCOUNT_ID_ARG}"

/** Route to open Transactions pre-filtered to a single account (used from "tap an account"); pass null for the plain tab. */
private fun transactionsRoute(accountId: Int?): String =
    if (accountId == null) Routes.TRANSACTIONS else "${Routes.TRANSACTIONS}?$ACCOUNT_ID_ARG=$accountId"

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Filled.List),
    BottomTab(Routes.ACCOUNTS, "Accounts", Icons.Filled.AccountBalance),
    BottomTab(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    BottomTab(Routes.MORE, "More", Icons.Filled.MoreHoriz),
)

@Composable
fun FinTrackNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val startDestination = if (SessionManager.isLoggedIn(context)) Routes.TRANSACTIONS else Routes.LOGIN

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("?")
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route?.substringBefore("?") == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(onLoggedIn = {
                        navController.navigate(Routes.TRANSACTIONS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    })
                }
                composable(
                    route = TRANSACTIONS_ROUTE,
                    arguments = listOf(navArgument(ACCOUNT_ID_ARG) { type = NavType.IntType; defaultValue = -1 })
                ) { entry ->
                    val argAccountId = entry.arguments?.getInt(ACCOUNT_ID_ARG)?.takeIf { it != -1 }
                    TransactionsScreen(initialAccountId = argAccountId)
                }
                composable(Routes.ACCOUNTS) {
                    AccountsScreen(onOpenAccountTransactions = { account ->
                        // Deliberately not launchSingleTop/restoreState here: this is a drill-down (a
                        // fresh, filtered view), not a bottom-tab switch — it pushes a new Transactions
                        // entry with its own ViewModel so the bottom-tab's own filter state, further
                        // down the back stack, is left untouched. Back navigates to Accounts as expected.
                        navController.navigate(transactionsRoute(account.id))
                    })
                }
                composable(Routes.DASHBOARD) { DashboardScreen() }
                composable(Routes.MORE) {
                    MoreScreen(
                        onOpenBudgets = { navController.navigate(Routes.BUDGETS) },
                        onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                        onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                        onOpenTransfers = { navController.navigate(Routes.TRANSFERS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) }
                    )
                }
                composable(Routes.BUDGETS) { BudgetsScreen() }
                composable(Routes.CATEGORIES) { CategoriesScreen() }
                composable(Routes.RECURRING) { RecurringScreen() }
                composable(Routes.TRANSFERS) { TransfersScreen() }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    })
                }
                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }

            // App-wide "connecting to server / data refreshing / offline" indicator, top-right.
            SyncStatusPill(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 8.dp, end = 12.dp)
            )
        }
    }
}
