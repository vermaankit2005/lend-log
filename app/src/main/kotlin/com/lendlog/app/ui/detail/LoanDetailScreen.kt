package com.lendlog.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.components.LoanStatusBadge
import com.lendlog.app.ui.components.TagChip
import com.lendlog.app.ui.theme.*
import com.lendlog.app.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onNavigateBack()
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Delete this loan?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
            text = { Text("This permanently removes the loan record and cannot be undone.", style = MaterialTheme.typography.bodyMedium, color = N500) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteLoan,
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger)
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = N0
        )
    }

    if (uiState.showReturnDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReturnDialog,
            title = { Text("Mark as returned?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
            text = { Text("This moves the loan to your history.", style = MaterialTheme.typography.bodyMedium, color = N500) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::markReturned,
                    colors = ButtonDefaults.textButtonColors(contentColor = Ink)
                ) { Text("Confirm", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissReturnDialog) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = N0
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onNavigateToAdd,
                containerColor = Ink,
                contentColor   = Color.White,
                shape          = CircleShape,
                elevation      = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Loan", modifier = Modifier.size(26.dp))
            }
        },
        topBar = {
            Column {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = N800)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = N600)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = N0
                            ) {
                                if (uiState.loan?.isReturned == false) {
                                    DropdownMenuItem(
                                        text = { Text("Edit loan", style = MaterialTheme.typography.bodyMedium) },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = { showMenu = false; onNavigateToEdit(loanId) }
                                    )
                                    HorizontalDivider(color = N100)
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete loan", color = Danger, style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Danger, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMenu = false; viewModel.showDeleteDialog() }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = N50)
                )
                HorizontalDivider(color = N200, thickness = 1.dp)
            }
        },
        containerColor = N50
    ) { padding ->
        val loan = uiState.loan
        if (loan == null) {
            // Skeleton
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonBox(height = 220.dp, radius = 12.dp)
                SkeletonBox(height = 32.dp, fraction = 0.6f, radius = 8.dp)
                SkeletonBox(height = 120.dp, radius = 12.dp)
                SkeletonBox(height = 52.dp, radius = 12.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                loan.photoUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = loan.itemName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                // Headline row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = loan.itemName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = N800,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    LoanStatusBadge(isOverdue = loan.isOverdue, isReturned = loan.isReturned)
                }

                // Info card
                DetailCard(loan = loan)

                // Notes
                if (!loan.notes.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EyebrowLabel("NOTES")
                        Surface(
                            color = N0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = loan.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = N700,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Tags
                if (loan.tagList.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        loan.tagList.forEach { TagChip(it) }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Actions
                if (!loan.isReturned) {
                    loan.borrowerPhone?.let { phone ->
                        OutlinedButton(
                            onClick = { WhatsAppHelper.sendNudge(context, phone, loan.itemName) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send a nudge", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Button(
                        onClick = viewModel::showReturnDialog,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Returned", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EyebrowLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = N500)
}

@Composable
private fun DetailCard(loan: Loan) {
    Surface(
        color = N0,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            DetailRow(
                icon = Icons.Outlined.Person,
                label = "BORROWER",
                value = loan.borrowerName
            )
            HorizontalDivider(color = N100, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
            DetailRow(
                icon = Icons.Outlined.CalendarMonth,
                label = "LENT ON",
                value = formatDate(loan.lentDate)
            )
            HorizontalDivider(color = N100, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
            DetailRow(
                icon = Icons.Outlined.Event,
                label = "DUE ON",
                value = formatDate(loan.returnDate),
                valueColor = if (loan.isOverdue && !loan.isReturned) Danger else null
            )
            if (loan.returnedDate != null) {
                HorizontalDivider(color = N100, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                DetailRow(
                    icon = Icons.Outlined.CheckCircle,
                    label = "RETURNED ON",
                    value = formatDate(loan.returnedDate),
                    valueColor = Success
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(N100, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = N500, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = N500)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor ?: N800
            )
        }
    }
}

@Composable
private fun SkeletonBox(height: androidx.compose.ui.unit.Dp, fraction: Float = 1f, radius: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .background(N100, RoundedCornerShape(radius))
    )
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))
