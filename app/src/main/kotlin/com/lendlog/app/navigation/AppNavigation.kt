package com.lendlog.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lendlog.app.ui.addloan.AddLoanScreen
import com.lendlog.app.ui.detail.LoanDetailScreen
import com.lendlog.app.ui.history.HistoryScreen
import com.lendlog.app.ui.home.HomeScreen
import com.lendlog.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val ADD_LOAN = "add"
    const val LOAN_DETAIL = "detail/{loanId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun loanDetail(loanId: String) = "detail/$loanId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToAdd = { navController.navigate(Routes.ADD_LOAN) },
                onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Routes.loanDetail(id)) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
