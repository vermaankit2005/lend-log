package com.lendlog.app.ui.home

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.components.EmptyState
import com.lendlog.app.ui.components.LendLogTopBar
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
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = { LendLogTopBar(showLogo = true) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
        ) {
            FeedToggle(
                feedView      = uiState.feedView,
                onFeedViewChange = viewModel::setFeedView
            )

            if (uiState.loans.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon      = Icons.Outlined.Inventory2,
                        title     = "Nothing lent out",
                        body      = "Snap a photo when you lend something — we'll remind you when it's due back.",
                        ctaLabel  = "Log your first loan",
                        onCtaClick = onNavigateToAdd
                    )
                }
            } else {
                when (uiState.feedView) {
                    FeedView.BY_STATUS -> ByItemFeed(uiState = uiState, onLoanClick = onNavigateToDetail)
                    FeedView.BY_PERSON -> ByPersonFeed(grouped = uiState.groupedByPerson, onLoanClick = onNavigateToDetail)
                }
            }
        }
    }
}

@Composable
private fun FeedToggle(feedView: FeedView, onFeedViewChange: (FeedView) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SegmentedButton(
            selected = feedView == FeedView.BY_STATUS,
            onClick  = { onFeedViewChange(FeedView.BY_STATUS) },
            shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors   = SegmentedButtonDefaults.colors(
                activeContainerColor = BrandSoft,
                activeContentColor   = BrandDeep,
                activeBorderColor    = Brand
            ),
            icon = {}
        ) { Text("By Status", style = MaterialTheme.typography.labelLarge) }

        SegmentedButton(
            selected = feedView == FeedView.BY_PERSON,
            onClick  = { onFeedViewChange(FeedView.BY_PERSON) },
            shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors   = SegmentedButtonDefaults.colors(
                activeContainerColor = BrandSoft,
                activeContentColor   = BrandDeep,
                activeBorderColor    = Brand
            ),
            icon = {}
        ) { Text("By Person", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, dotColor: androidx.compose.ui.graphics.Color? = null) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
        }
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = N500,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text  = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = N400
        )
    }
}

@Composable
private fun ByItemFeed(uiState: HomeUiState, onLoanClick: (String) -> Unit) {
    val overdueLoans = uiState.overdueLoans
    val dueSoonLoans = uiState.dueSoonLoans
    val activeLoans  = uiState.activeLoans

    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (overdueLoans.isNotEmpty()) {
            item(key = "header_overdue") {
                SectionHeader(label = "Overdue", count = overdueLoans.size, dotColor = Danger)
            }
            itemsIndexed(overdueLoans, key = { _, loan -> "overdue_${loan.id}" }) { index, loan ->
                AnimatedLoanCard(index = index, loan = loan, onLoanClick = onLoanClick)
            }
        }

        if (dueSoonLoans.isNotEmpty()) {
            item(key = "header_due_soon") {
                SectionHeader(label = "Due Soon", count = dueSoonLoans.size, dotColor = Warning)
            }
            itemsIndexed(dueSoonLoans, key = { _, loan -> "soon_${loan.id}" }) { index, loan ->
                AnimatedLoanCard(index = index, loan = loan, onLoanClick = onLoanClick)
            }
        }

        if (activeLoans.isNotEmpty()) {
            item(key = "header_active") {
                SectionHeader(label = "Active", count = activeLoans.size)
            }
            itemsIndexed(activeLoans, key = { _, loan -> "active_${loan.id}" }) { index, loan ->
                AnimatedLoanCard(index = index, loan = loan, onLoanClick = onLoanClick)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun AnimatedLoanCard(index: Int, loan: Loan, onLoanClick: (String) -> Unit) {
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

@Composable
private fun ByPersonFeed(grouped: Map<String, List<Loan>>, onLoanClick: (String) -> Unit) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (person, loans) ->
            item(key = "header_$person") {
                PersonGroupHeader(person = person, loans = loans)
            }
            itemsIndexed(loans, key = { _, loan -> loan.id }) { _, loan ->
                LoanCard(loan = loan, onClick = { onLoanClick(loan.id) })
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun PersonGroupHeader(person: String, loans: List<Loan>) {
    val overdueCount = loans.count { it.isOverdue }
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text  = person.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = N500,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = "${loans.size} item${if (loans.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = N400
            )
        }
        if (overdueCount > 0) {
            Surface(color = DangerSoft, shape = MaterialTheme.shapes.small) {
                Text(
                    text     = "$overdueCount overdue",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Danger,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
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
