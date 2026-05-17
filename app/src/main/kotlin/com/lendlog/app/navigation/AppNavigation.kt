package com.lendlog.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.lendlog.app.ui.theme.Ink

object Routes {
    const val HOME       = "home"
    const val ADD_LOAN   = "add"
    const val LOAN_DETAIL = "detail/{loanId}"
    const val HISTORY    = "history"
    const val SETTINGS   = "settings"

    fun loanDetail(loanId: String) = "detail/$loanId"
}

private data class NavItem(
    val route:        String,
    val label:        String,
    val activeIcon:   ImageVector,
    val inactiveIcon: ImageVector
)

private val navItems = listOf(
    NavItem(Routes.HOME,     "Loans",    Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    NavItem(Routes.HISTORY,  "History",  Icons.Filled.History,  Icons.Outlined.History),
    NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

private val bottomNavRoutes = navItems.map { it.route }.toSet()

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                FloatingActionButton(
                    onClick        = { navController.navigate(Routes.ADD_LOAN) },
                    containerColor = Ink,          // Ink is now Brand (teal)
                    contentColor   = Color.White,
                    shape          = CircleShape,
                    elevation      = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Loan", modifier = Modifier.size(26.dp))
                }
            }
        },
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 1.dp
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick  = {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector        = if (selected) item.activeIcon else item.inactiveIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label  = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.HOME
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToAdd    = { navController.navigate(Routes.ADD_LOAN) },
                    onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) },
                    bottomPadding      = padding
                )
            }

            composable(Routes.ADD_LOAN) {
                AddLoanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLoanSaved    = { navController.popBackStack() }
                )
            }

            composable(
                route     = Routes.LOAN_DETAIL,
                arguments = listOf(navArgument("loanId") { type = NavType.StringType })
            ) { backStack ->
                val loanId = backStack.arguments?.getString("loanId") ?: return@composable
                LoanDetailScreen(
                    loanId         = loanId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAdd = { navController.navigate(Routes.ADD_LOAN) }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) },
                    bottomPadding      = padding
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(bottomPadding = padding)
            }
        }
    }
}
