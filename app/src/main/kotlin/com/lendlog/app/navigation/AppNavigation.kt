package com.lendlog.app.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.lendlog.app.ui.theme.Ink

object Routes {
    const val HOME        = "home"
    const val ADD_LOAN    = "add"
    const val LOAN_DETAIL = "detail/{loanId}"
    const val EDIT_LOAN   = "edit/{loanId}"
    const val HISTORY     = "history"
    const val SETTINGS    = "settings"

    fun loanDetail(loanId: String) = "detail/$loanId"
    fun editLoan(loanId: String)   = "edit/$loanId"
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

// Shared transition specs
private val pushEnter  = slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn()
private val pushExit   = slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 4 } + fadeOut()
private val popEnter   = slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 4 } + fadeIn()
private val popExit    = slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut()
private val tabEnter   = fadeIn()
private val tabExit    = fadeOut()

@Composable
fun AppNavigation(deepLinkLoanId: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(deepLinkLoanId) {
        if (deepLinkLoanId != null) {
            navController.navigate(Routes.loanDetail(deepLinkLoanId))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
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
            startDestination = Routes.HOME,
            enterTransition  = { tabEnter },
            exitTransition   = { tabExit },
            popEnterTransition  = { tabEnter },
            popExitTransition   = { tabExit }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToAdd    = { navController.navigate(Routes.ADD_LOAN) },
                    onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) },
                    bottomPadding      = padding
                )
            }

            composable(
                route            = Routes.ADD_LOAN,
                enterTransition  = { pushEnter },
                exitTransition   = { pushExit },
                popEnterTransition  = { popEnter },
                popExitTransition   = { popExit }
            ) {
                AddLoanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLoanSaved    = { navController.popBackStack() }
                )
            }

            composable(
                route     = Routes.LOAN_DETAIL,
                arguments = listOf(navArgument("loanId") { type = NavType.StringType }),
                enterTransition  = { pushEnter },
                exitTransition   = { pushExit },
                popEnterTransition  = { popEnter },
                popExitTransition   = { popExit }
            ) { backStack ->
                val loanId = backStack.arguments?.getString("loanId") ?: return@composable
                LoanDetailScreen(
                    loanId            = loanId,
                    onNavigateBack    = { navController.popBackStack() },
                    onNavigateToAdd   = { navController.navigate(Routes.ADD_LOAN) },
                    onNavigateToEdit  = { id -> navController.navigate(Routes.editLoan(id)) }
                )
            }

            composable(
                route     = Routes.EDIT_LOAN,
                arguments = listOf(navArgument("loanId") { type = NavType.StringType }),
                enterTransition  = { pushEnter },
                exitTransition   = { pushExit },
                popEnterTransition  = { popEnter },
                popExitTransition   = { popExit }
            ) {
                AddLoanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLoanSaved    = { navController.popBackStack() }
                )
            }

            composable(
                route            = Routes.HISTORY,
                enterTransition  = { tabEnter },
                exitTransition   = { tabExit },
                popEnterTransition  = { tabEnter },
                popExitTransition   = { tabExit }
            ) {
                HistoryScreen(
                    onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) },
                    bottomPadding      = padding
                )
            }

            composable(
                route            = Routes.SETTINGS,
                enterTransition  = { tabEnter },
                exitTransition   = { tabExit },
                popEnterTransition  = { tabEnter },
                popExitTransition   = { tabExit }
            ) {
                SettingsScreen(bottomPadding = padding)
            }
        }
    }
}
