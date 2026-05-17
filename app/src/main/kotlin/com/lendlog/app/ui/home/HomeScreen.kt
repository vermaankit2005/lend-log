package com.lendlog.app.ui.home

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.components.EmptyState
import com.lendlog.app.ui.components.LoanCard
import com.lendlog.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RequestNotificationPermission()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Ink, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "L",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            "LendLog",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = N800
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = N50
                )
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
            // View toggle
            FeedToggle(
                feedView = uiState.feedView,
                onFeedViewChange = viewModel::setFeedView
            )

            // Summary + overdue filter
            if (uiState.loans.isNotEmpty()) {
                SummaryRow(
                    totalCount = uiState.loans.size,
                    overdueCount = uiState.loans.count { it.isOverdue },
                    filter = uiState.filter,
                    onFilterChange = viewModel::setFilter
                )
            }

            if (uiState.loans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = "No active loans",
                        body = "When you lend something, it'll show up here so you don't forget.",
                        ctaLabel = "Add a loan",
                        onCtaClick = onNavigateToAdd
                    )
                }
            } else {
                when (uiState.feedView) {
                    FeedView.BY_ITEM -> ByItemFeed(
                        loans = uiState.displayedLoans,
                        onLoanClick = onNavigateToDetail
                    )
                    FeedView.BY_PERSON -> ByPersonFeed(
                        grouped = uiState.groupedByPerson,
                        onLoanClick = onNavigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedToggle(
    feedView: FeedView,
    onFeedViewChange: (FeedView) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SegmentedButton(
            selected = feedView == FeedView.BY_ITEM,
            onClick = { onFeedViewChange(FeedView.BY_ITEM) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = InkSoft,
                activeContentColor = Ink,
                activeBorderColor = Ink
            ),
            icon = {}
        ) {
            Text("By Item", style = MaterialTheme.typography.labelLarge)
        }
        SegmentedButton(
            selected = feedView == FeedView.BY_PERSON,
            onClick = { onFeedViewChange(FeedView.BY_PERSON) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = InkSoft,
                activeContentColor = Ink,
                activeBorderColor = Ink
            ),
            icon = {}
        ) {
            Text("By Person", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SummaryRow(
    totalCount: Int,
    overdueCount: Int,
    filter: FilterType,
    onFilterChange: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "$totalCount active",
                style = MaterialTheme.typography.bodyMedium,
                color = N500
            )
            if (overdueCount > 0) {
                Text("·", style = MaterialTheme.typography.bodyMedium, color = N400)
                Text(
                    text = "$overdueCount overdue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Danger,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (overdueCount > 0) {
            FilterChip(
                selected = filter == FilterType.OVERDUE,
                onClick = {
                    onFilterChange(if (filter == FilterType.OVERDUE) FilterType.ALL else FilterType.OVERDUE)
                },
                label = { Text("Overdue only", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DangerSoft,
                    selectedLabelColor = Danger
                )
            )
        }
    }
}

@Composable
private fun ByItemFeed(loans: List<Loan>, onLoanClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp, ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(loans, key = { _, loan -> loan.id }) { index, loan ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(minOf(index * 40L, 240L))
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                LoanCard(loan = loan, onClick = { onLoanClick(loan.id) })
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ByPersonFeed(grouped: Map<String, List<Loan>>, onLoanClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (person, loans) ->
            item(key = "header_$person") {
                Text(
                    text = person.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = N500,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(loans, key = { _, loan -> loan.id }) { _, loan ->
                LoanCard(loan = loan, onClick = { onLoanClick(loan.id) })
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val permission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }
}
