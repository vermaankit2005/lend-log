package com.lendlog.app.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.components.EmptyState
import com.lendlog.app.ui.components.LendLogTopBar
import com.lendlog.app.ui.components.LoanCard
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (String) -> Unit,
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val grouped = uiState.loans.groupBy { loan ->
        val ts = loan.returnedDate ?: loan.lentDate
        monthFmt.format(Date(ts))
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = { LendLogTopBar(title = "History") },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
        ) {
            if (uiState.loans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.History,
                        title = "No returned loans yet",
                        body = "Loans you mark as returned will appear here."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var globalIndex = 0
                    grouped.forEach { (month, loans) ->
                        item(key = "header_$month") {
                            Text(
                                text = month.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        val sectionStart = globalIndex
                        itemsIndexed(loans, key = { _, loan -> loan.id }) { localIndex, loan ->
                            AnimatedHistoryCard(
                                index = sectionStart + localIndex,
                                loan = loan,
                                onLoanClick = onNavigateToDetail
                            )
                        }
                        globalIndex += loans.size
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AnimatedHistoryCard(index: Int, loan: Loan, onLoanClick: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minOf(index * 40L, 240L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        LoanCard(loan = loan, onClick = { onLoanClick(loan.id) })
    }
}
