package com.lendlog.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lendlog.app.ui.addloan.AddLoanScreen
import com.lendlog.app.ui.detail.LoanDetailScreen
import com.lendlog.app.ui.history.HistoryScreen
import com.lendlog.app.ui.home.HomeScreen
import com.lendlog.app.ui.settings.SettingsScreen
import com.lendlog.app.ui.theme.TealPrimary

object Routes {
    const val HOME = "home"
    const val ADD_LOAN = "add"
    const val LOAN_DETAIL = "detail/{loanId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun loanDetail(loanId: String) = "detail/$loanId"
}

private data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, Icons.Outlined.Home, "Loans"),
    BottomNavItem(Routes.HISTORY, Icons.Outlined.History, "History"),
    BottomNavItem(Routes.SETTINGS, Icons.Outlined.Settings, "Settings")
)

private val bottomNavRoutes = setOf(Routes.HOME, Routes.HISTORY, Routes.SETTINGS)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                indicatorColor = TealPrimary.copy(alpha = 0.10f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToAdd = { navController.navigate(Routes.ADD_LOAN) },
                    onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) },
                    bottomPadding = padding
                )
            }

            composable(Routes.ADD_LOAN) {
                AddLoanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLoanSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.LOAN_DETAIL,
                arguments = listOf(navArgument("loanId") { type = NavType.StringType })
            ) { backStack ->
                val loanId = backStack.arguments?.getString("loanId") ?: return@composable
                LoanDetailScreen(
                    loanId = loanId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) },
                    bottomPadding = padding
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(bottomPadding = padding)
            }
        }
    }
}
