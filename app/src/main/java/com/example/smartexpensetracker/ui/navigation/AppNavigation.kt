package com.example.smartexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartexpensetracker.ui.components.AddEditTransactionDialog
import com.example.smartexpensetracker.ui.screens.auth.LoginScreen
import com.example.smartexpensetracker.ui.screens.budget.BudgetScreen
import com.example.smartexpensetracker.ui.screens.dashboard.DashboardScreen
import com.example.smartexpensetracker.ui.screens.reports.ReportsScreen
import com.example.smartexpensetracker.ui.screens.settings.SettingsScreen
import com.example.smartexpensetracker.ui.screens.transactions.TransactionsScreen
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.ListAlt)
    object Budget : Screen("budget", "Budget", Icons.Default.AccountBalanceWallet)
    object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { /* Navigates to main screen automatically */ }
        )
    } else {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        var showAddDialog by remember { mutableStateOf(false) }
        val categories by viewModel.categories.collectAsState()

        val screens = listOf(
            Screen.Dashboard,
            Screen.Transactions,
            Screen.Budget,
            Screen.Reports,
            Screen.Settings
        )

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onAddTransactionClick = { showAddDialog = true },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToHelp = { navController.navigate("help_support") },
                        onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                        onNavigateToReports = { navController.navigate(Screen.Reports.route) }
                    )
                }
                composable(Screen.Transactions.route) {
                    TransactionsScreen(viewModel = viewModel, onAddTransactionClick = { showAddDialog = true })
                }
                composable(Screen.Budget.route) {
                    BudgetScreen(viewModel = viewModel)
                }
                composable(Screen.Reports.route) {
                    ReportsScreen(viewModel = viewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { navController.navigate("profile") }
                    )
                }
                composable("profile") {
                    com.example.smartexpensetracker.ui.screens.profile.ProfileScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("help_support") {
                    com.example.smartexpensetracker.ui.screens.support.HelpSupportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddEditTransactionDialog(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onSave = { amt, isInc, merch, cat, method, note ->
                    viewModel.addTransaction(amt, isInc, merch, cat, method, note)
                    showAddDialog = false
                }
            )
        }
    }
}
