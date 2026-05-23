package com.lendlog.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lendlog.app.navigation.AppNavigation
import com.lendlog.app.ui.theme.LendLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLinkLoanId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkLoanId = intent.getStringExtra(EXTRA_LOAN_ID)
        setContent {
            LendLogTheme {
                AppNavigation(deepLinkLoanId = deepLinkLoanId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkLoanId = intent.getStringExtra(EXTRA_LOAN_ID)
    }

    companion object {
        const val EXTRA_LOAN_ID = "extra_loan_id"
    }
}
