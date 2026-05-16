package com.lendlog.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.lendlog.app.ui.components.StatusBadge
import com.lendlog.app.ui.components.TagChip
import com.lendlog.app.ui.theme.MutedText
import com.lendlog.app.ui.theme.OverdueRed
import com.lendlog.app.ui.theme.TealPrimary
import com.lendlog.app.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: String,
    onNavigateBack: () -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onNavigateBack()
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Delete this loan?") },
            text = { Text("This will permanently remove the loan record. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteLoan,
                    colors = ButtonDefaults.textButtonColors(contentColor = OverdueRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (uiState.showReturnDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReturnDialog,
            title = { Text("Mark as returned?") },
            text = { Text("This will move the loan to your history.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::markReturned,
                    colors = ButtonDefaults.textButtonColors(contentColor = TealPrimary)
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissReturnDialog) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showDeleteDialog) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = OverdueRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val loan = uiState.loan
        if (loan == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = loan.itemName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (loan.isReturned) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                "Returned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        StatusBadge(isOverdue = loan.isOverdue)
                    }
                }

                DetailInfoCard(loan = loan)

                if (loan.notes != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedText
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(loan.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (loan.tagList.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        loan.tagList.forEach { TagChip(it) }
                    }
                }

                if (!loan.isReturned) {
                    loan.borrowerPhone?.let { phone ->
                        OutlinedButton(
                            onClick = { WhatsAppHelper.sendNudge(context, phone, loan.itemName) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Nudge via WhatsApp")
                        }
                    }

                    Button(
                        onClick = viewModel::showReturnDialog,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Returned")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoCard(loan: Loan) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow(
                icon = Icons.Outlined.Person,
                label = "Borrower",
                value = loan.borrowerName
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            DetailRow(
                icon = Icons.Outlined.CalendarMonth,
                label = "Lent on",
                value = formatDate(loan.lentDate)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            DetailRow(
                icon = Icons.Outlined.Event,
                label = "Due on",
                value = formatDate(loan.returnDate),
                valueColor = if (loan.isOverdue) OverdueRed else null
            )
            if (loan.returnedDate != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                DetailRow(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Returned on",
                    value = formatDate(loan.returnedDate),
                    valueColor = MaterialTheme.colorScheme.tertiary
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(TealPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MutedText)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))
