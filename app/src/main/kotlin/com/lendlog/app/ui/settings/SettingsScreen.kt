package com.lendlog.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lendlog.app.BuildConfig
import com.lendlog.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { success ->
            snackbarHostState.showSnackbar(if (success) "Backup saved to Downloads" else "Export failed. Try again.")
            viewModel.clearExportResult()
        }
    }

    LaunchedEffect(uiState.restoreResult) {
        uiState.restoreResult?.let { success ->
            snackbarHostState.showSnackbar(if (success) "Backup restored successfully" else "Could not read backup file")
            viewModel.clearRestoreResult()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                viewModel.restoreFromJson(stream.bufferedReader().readText())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = N800
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = N50)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = N50
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        ) {
            // Plan section
            SectionHeader("PLAN")
            SettingsGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (uiState.isUnlocked) "Unlimited" else "Free",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = N800
                        )
                        Text(
                            text = if (uiState.isUnlocked) "All features unlocked" else "Up to 3 active loans",
                            style = MaterialTheme.typography.bodySmall,
                            color = N500
                        )
                    }
                    Surface(
                        color = if (uiState.isUnlocked) SuccessSoft else InkSoft,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (uiState.isUnlocked) "Pro" else "Free",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.isUnlocked) Success else Ink,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Backup section
            SectionHeader("BACKUP")
            SettingsGroup {
                if (uiState.lastBackupTimestamp != 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Last backup: ${formatDateTime(uiState.lastBackupTimestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = N500
                        )
                    }
                    HorizontalDivider(color = N200, thickness = 1.dp)
                }

                SettingsRow(
                    icon = Icons.Outlined.Upload,
                    iconTint = Ink,
                    title = "Export backup now",
                    subtitle = "Save to Downloads folder",
                    onClick = viewModel::exportNow,
                    loading = uiState.isExporting
                )

                HorizontalDivider(color = N200, thickness = 1.dp)

                SettingsRow(
                    icon = Icons.Outlined.Download,
                    iconTint = Ink,
                    title = "Restore from backup",
                    subtitle = "Pick a lendlog-backup.json file",
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                    loading = uiState.isRestoring
                )
            }

            Spacer(Modifier.height(24.dp))

            // About section
            SectionHeader("ABOUT")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    iconTint = N400,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = {},
                    showChevron = false
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = N500,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = N0,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    showChevron: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = N800)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = N500)
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
            } else if (showChevron) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = N400, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(epochMillis))
