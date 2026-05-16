package com.lendlog.app.ui.home

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.components.EmptyState
import com.lendlog.app.ui.components.LoanCard
import com.lendlog.app.ui.theme.MutedText
import com.lendlog.app.ui.theme.TealPrimary
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
                    Text(
                        "LendLog",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Loan")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
        ) {
            FeedControls(
                feedView = uiState.feedView,
                filter = uiState.filter,
                onFeedViewChange = viewModel::setFeedView,
                onFilterChange = viewModel::setFilter
            )

            if (uiState.loans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = "Nothing lent out",
                        body = "Tap + to log your first loan",
                        ctaLabel = "Log a loan",
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
private fun FeedControls(
    feedView: FeedView,
    filter: FilterType,
    onFeedViewChange: (FeedView) -> Unit,
    onFilterChange: (FilterType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ViewToggleChip(
                label = "By Item",
                selected = feedView == FeedView.BY_ITEM,
                onClick = { onFeedViewChange(FeedView.BY_ITEM) },
                modifier = Modifier.weight(1f)
            )
            ViewToggleChip(
                label = "By Person",
                selected = feedView == FeedView.BY_PERSON,
                onClick = { onFeedViewChange(FeedView.BY_PERSON) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == FilterType.ALL,
                onClick = { onFilterChange(FilterType.ALL) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealPrimary.copy(alpha = 0.10f),
                    selectedLabelColor = TealPrimary
                )
            )
            FilterChip(
                selected = filter == FilterType.OVERDUE,
                onClick = { onFilterChange(FilterType.OVERDUE) },
                label = { Text("Overdue") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    selectedLabelColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
private fun ViewToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) TealPrimary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TealPrimary else MutedText
            )
        }
    }
}

@Composable
private fun ByItemFeed(
    loans: List<Loan>,
    onLoanClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(loans, key = { _, loan -> loan.id }) { index, loan ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 60L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                LoanCard(loan = loan, onClick = { onLoanClick(loan.id) })
            }
        }
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

@Composable
private fun ByPersonFeed(
    grouped: Map<String, List<Loan>>,
    onLoanClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (person, loans) ->
            item(key = "header_$person") {
                Text(
                    text = person,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(loans, key = { _, loan -> loan.id }) { _, loan ->
                LoanCard(loan = loan, onClick = { onLoanClick(loan.id) })
            }
        }
    }
}
