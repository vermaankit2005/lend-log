package com.lendlog.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.components.EmptyState
import com.lendlog.app.ui.components.LoanCard
import com.lendlog.app.ui.theme.TealDeep
import com.lendlog.app.ui.theme.TealLight
import com.lendlog.app.ui.theme.TealPrimary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LendLog",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Outlined.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
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
        ) {
            FeedControls(
                feedView = uiState.feedView,
                filter = uiState.filter,
                onFeedViewChange = viewModel::setFeedView,
                onFilterChange = viewModel::setFilter
            )

            if (uiState.loans.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "Nothing lent out",
                    body = "Tap the + button to log your first loan",
                    ctaLabel = "Log a loan",
                    onCtaClick = onNavigateToAdd,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
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
        // View toggle
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

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == FilterType.ALL,
                onClick = { onFilterChange(FilterType.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = filter == FilterType.OVERDUE,
                onClick = { onFilterChange(FilterType.OVERDUE) },
                label = { Text("Overdue") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
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
        color = if (selected) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = if (selected) null else ButtonDefaults.outlinedButtonBorder
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
                color = if (selected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                delay(index * 70L)
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
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(loans, key = { _, loan -> loan.id }) { _, loan ->
                LoanCard(loan = loan, onClick = { onLoanClick(loan.id) })
            }
        }
    }
}
