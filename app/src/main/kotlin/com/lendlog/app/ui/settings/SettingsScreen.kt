package com.lendlog.app.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lendlog.app.BuildConfig
import com.lendlog.app.ui.components.LendLogTopBar
import com.lendlog.app.ui.paywall.PaywallSheet
import com.lendlog.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf(false) }
    val writeStoragePermission = rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val sendSmsPermission = rememberPermissionState(Manifest.permission.SEND_SMS) { granted ->
        if (granted) {
            viewModel.setAutoSmsEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("SMS permission is required for Auto SMS") }
        }
    }

    LaunchedEffect(writeStoragePermission.status, pendingExport) {
        if (pendingExport && writeStoragePermission.status.isGranted) {
            pendingExport = false
            viewModel.exportNow()
        }
    }

    val handleExport: () -> Unit = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !writeStoragePermission.status.isGranted) {
            pendingExport = true
            writeStoragePermission.launchPermissionRequest()
        } else {
            viewModel.exportNow()
        }
    }

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

    if (uiState.showPaywall) {
        PaywallSheet(
            onDismiss = viewModel::dismissPaywall,
            onPurchased = viewModel::onPurchased
        )
    }

    if (uiState.showAutoSmsConfirm) {
        AutoSmsConfirmSheet(
            onDismiss = viewModel::dismissAutoSmsConfirm,
            onConfirm = {
                viewModel.confirmAutoSms()
                if (sendSmsPermission.status.isGranted) {
                    viewModel.setAutoSmsEnabled(true)
                } else {
                    sendSmsPermission.launchPermissionRequest()
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = { LendLogTopBar(title = "Settings") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        ) {

            // ── PLAN ──────────────────────────────────────────────────────
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (uiState.isUnlocked) "All features unlocked" else "Up to 3 active loans",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = if (uiState.isUnlocked)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (uiState.isUnlocked) "Pro" else "Free",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.isUnlocked)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!uiState.isUnlocked) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    SettingsRow(
                        icon = Icons.Outlined.WorkspacePremium,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Upgrade to Unlimited",
                        subtitle = "One-time payment of \$2.99",
                        onClick = viewModel::showPaywall
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── NOTIFICATIONS ─────────────────────────────────────────────
            SectionHeader("NOTIFICATIONS")
            SettingsGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Loan reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Notify before and when loans are overdue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled
                    )
                }

                if (uiState.notificationsEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "Remind me",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "Days before the due date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf(1, 3, 7).forEachIndexed { i, days ->
                                SegmentedButton(
                                    selected = uiState.reminderDays == days,
                                    onClick = { viewModel.setReminderDays(days) },
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                                    icon = {}
                                ) {
                                    Text(
                                        text = "$days ${if (days == 1) "day" else "days"}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── NUDGES ────────────────────────────────────────────────────
            SectionHeader("NUDGES")

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "About Auto SMS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        "When enabled, LendLog will automatically send an SMS to the borrower " +
                        "3 days before the due date and again when a loan goes overdue. " +
                        "The message is sent from your number — the borrower will see it as a " +
                        "normal text from you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Outlined.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto SMS reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Automatically text borrowers when loans are due",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.autoSmsEnabled,
                        onCheckedChange = { viewModel.onAutoSmsToggled(it) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── BACKUP ────────────────────────────────────────────────────
            SectionHeader("BACKUP")

            // Info card
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "How backup works",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        "LendLog automatically backs up your loans every night to a file called " +
                        "lendlog-backup.json in your Downloads folder. The file stays on your " +
                        "device — nothing is sent to the cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "If you switch phones or reinstall the app, tap Restore from backup and " +
                        "pick that file to get all your loans back.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsGroup {
                if (uiState.lastBackupTimestamp != 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Last backup: ${formatDateTime(uiState.lastBackupTimestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                }

                SettingsRow(
                    icon = Icons.Outlined.Upload,
                    iconTint = MaterialTheme.colorScheme.onBackground,
                    title = "Export backup now",
                    subtitle = "Saves lendlog-backup.json to Downloads",
                    onClick = handleExport,
                    loading = uiState.isExporting
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                SettingsRow(
                    icon = Icons.Outlined.Download,
                    iconTint = MaterialTheme.colorScheme.onBackground,
                    title = "Restore from backup",
                    subtitle = "Pick a lendlog-backup.json to import",
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                    loading = uiState.isRestoring
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── ABOUT ─────────────────────────────────────────────────────
            SectionHeader("ABOUT")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = {},
                    showChevron = false
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                SettingsRow(
                    icon = Icons.Outlined.Star,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Rate on Play Store",
                    subtitle = "Enjoying LendLog? Leave a review",
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            )
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
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
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else if (showChevron) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(epochMillis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSmsConfirmSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = N0,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Enable Auto SMS Reminders?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = N800
            )

            Text(
                "When enabled, LendLog will send an SMS on your behalf to each borrower:",
                style = MaterialTheme.typography.bodyMedium,
                color = N600
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BulletPoint("3 days before the return date as a heads-up")
                BulletPoint("On the due date if the loan is still active")
            }

            Surface(
                color = BrandSoft,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Sample message", style = MaterialTheme.typography.labelSmall, color = BrandDeep)
                    Text(
                        "\"Hey! Just a reminder — you still have my [item]. Would love to get it back soon 😊\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandDeep
                    )
                }
            }

            Surface(
                color = WarningSoft,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Warning,
                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                    )
                    Text(
                        "The borrower sees your phone number as the sender. SMS permission will be requested.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Warning
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Not now", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = N0)
                ) {
                    Text("Enable & Allow", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = Brand)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = N700)
    }
}
