package com.lendlog.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lendlog.app.ui.components.EmptyState
import com.lendlog.app.ui.components.LoanCard
import com.lendlog.app.ui.theme.N200
import com.lendlog.app.ui.theme.N50
import com.lendlog.app.ui.theme.N500
import com.lendlog.app.ui.theme.N800
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

    // Group by "Month YYYY" using returnedDate (fall back to lentDate)
    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val grouped = uiState.loans.groupBy { loan ->
        val ts = loan.returnedDate ?: loan.lentDate
        monthFmt.format(Date(ts))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = N800
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = N50)
            )
        },
        containerColor = N50
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
                    grouped.forEach { (month, loans) ->
                        item(key = "header_$month") {
                            Text(
                                text = month.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = N500,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        itemsIndexed(loans, key = { _, loan -> loan.id }) { _, loan ->
                            LoanCard(loan = loan, onClick = { onNavigateToDetail(loan.id) })
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}
