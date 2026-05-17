package com.lendlog.app.ui.addloan

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lendlog.app.ui.components.TealGradientButton
import com.lendlog.app.ui.paywall.PaywallSheet
import com.lendlog.app.ui.theme.*
import com.lendlog.app.util.ContactPickerHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddLoanScreen(
    onNavigateBack: () -> Unit,
    onLoanSaved: () -> Unit,
    viewModel: AddLoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPhotoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedLoanId) {
        if (uiState.savedLoanId != null) onLoanSaved()
    }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraLaunch by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) viewModel.updatePhotoUri(tempCameraUri.toString())
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.updatePhotoUri(it.toString()) }
    }

    LaunchedEffect(cameraPermission.status.isGranted, pendingCameraLaunch) {
        if (pendingCameraLaunch && cameraPermission.status.isGranted) {
            pendingCameraLaunch = false
            val uri = createTempPhotoFile(context)
            tempCameraUri = uri
            uri?.let { cameraLauncher.launch(it) }
        }
    }

    val contactsPermission = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    var pendingContactPick by remember { mutableStateOf(false) }

    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        uri?.let { contactUri ->
            val (name, phone, contactId) = ContactPickerHelper.resolveContact(context, contactUri)
            if (name != null) viewModel.updateBorrower(name, contactId, phone)
        }
    }

    LaunchedEffect(contactsPermission.status.isGranted, pendingContactPick) {
        if (pendingContactPick && contactsPermission.status.isGranted) {
            pendingContactPick = false
            contactLauncher.launch(null)
        }
    }

    if (uiState.showPaywall) {
        PaywallSheet(
            onDismiss = viewModel::dismissPaywall,
            onPurchased = { viewModel.dismissPaywall(); viewModel.onPurchased() }
        )
    }

    if (showPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            containerColor = N0,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                PhotoSheetRow(
                    icon = Icons.Outlined.CameraAlt,
                    label = "Take a photo",
                    onClick = {
                        showPhotoSheet = false
                        if (cameraPermission.status.isGranted) {
                            tempCameraUri = createTempPhotoFile(context)
                            tempCameraUri?.let { cameraLauncher.launch(it) }
                        } else {
                            pendingCameraLaunch = true
                            cameraPermission.launchPermissionRequest()
                        }
                    }
                )
                HorizontalDivider(color = N200)
                PhotoSheetRow(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "Choose from library",
                    onClick = { showPhotoSheet = false; galleryLauncher.launch("image/*") }
                )
                if (uiState.photoUri != null) {
                    HorizontalDivider(color = N200)
                    PhotoSheetRow(
                        icon = Icons.Outlined.Delete,
                        label = "Remove photo",
                        tint = Danger,
                        onClick = { showPhotoSheet = false; viewModel.updatePhotoUri(null) }
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Loan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = N800) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = N800)
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Photo
            FormSection("PHOTO")
            PhotoDropzone(
                photoUri = uiState.photoUri,
                onClick = { showPhotoSheet = true }
            )

            // Item name
            FormSection("WHAT")
            FormField(
                value = uiState.itemName,
                onValueChange = viewModel::updateItemName,
                placeholder = "What are you lending?",
                leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = N400, modifier = Modifier.size(20.dp)) }
            )

            // Borrower
            FormSection("TO WHOM")
            FormField(
                value = uiState.borrowerName,
                onValueChange = { viewModel.updateBorrower(it) },
                placeholder = "Borrower's name",
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = N400, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            if (contactsPermission.status.isGranted) {
                                contactLauncher.launch(null)
                            } else {
                                pendingContactPick = true
                                contactsPermission.launchPermissionRequest()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(16.dp), tint = Ink)
                        Spacer(Modifier.width(4.dp))
                        Text("Contacts", style = MaterialTheme.typography.labelMedium, color = Ink)
                    }
                }
            )

            // Return date
            FormSection("WHEN DUE")
            DateField(
                selectedDate = uiState.returnDate,
                onDateSelected = viewModel::updateReturnDate,
                context = context
            )

            // Notes
            FormSection("NOTES")
            FormField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                placeholder = "Any notes (optional)",
                minLines = 3,
                maxLines = 5,
                leadingIcon = { Icon(Icons.Outlined.Notes, contentDescription = null, tint = N400, modifier = Modifier.size(20.dp)) }
            )

            // Tags
            FormSection("TAGS")
            FormField(
                value = uiState.tags,
                onValueChange = viewModel::updateTags,
                placeholder = "e.g. book, tools, electronics",
                leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null, tint = N400, modifier = Modifier.size(20.dp)) }
            )

            Spacer(Modifier.height(8.dp))

            TealGradientButton(
                text = if (uiState.isSaving) "Saving…" else "Save Loan",
                onClick = viewModel::saveLoan,
                enabled = uiState.isValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormSection(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = N500,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = N400, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = maxLines == 1,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = N0,
            unfocusedContainerColor = N0,
            focusedBorderColor = Ink,
            unfocusedBorderColor = N200,
            focusedTextColor = N800,
            unfocusedTextColor = N800,
            cursorColor = Ink
        )
    )
}

@Composable
private fun PhotoDropzone(photoUri: String?, onClick: () -> Unit) {
    if (photoUri != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Loan photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Change", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(N50, RoundedCornerShape(12.dp))
                .border(1.dp, N300, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = null,
                    tint = N400,
                    modifier = Modifier.size(28.dp)
                )
                Text("Add a photo", style = MaterialTheme.typography.labelLarge, color = N600)
                Text("Optional", style = MaterialTheme.typography.bodySmall, color = N400)
            }
        }
    }
}

@Composable
private fun PhotoSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = N800,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

@Composable
private fun DateField(
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit,
    context: Context
) {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()

    val displayText = selectedDate?.let { date ->
        val absDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(date))
        val daysUntil = TimeUnit.MILLISECONDS.toDays(date - now)
        val relText = when {
            daysUntil < 0  -> "overdue"
            daysUntil == 0L -> "today"
            daysUntil == 1L -> "tomorrow"
            else           -> "in $daysUntil days"
        }
        "$absDate · $relText"
    } ?: "Select a date"

    Box(modifier = Modifier.fillMaxWidth().clickable {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day, 23, 59, 59)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = System.currentTimeMillis() }.show()
    }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = N0,
                disabledBorderColor = N200,
                disabledTextColor = if (selectedDate != null) N800 else N400,
                disabledLeadingIconColor = N400,
                disabledTrailingIconColor = N400,
                disabledPlaceholderColor = N400
            )
        )
    }
}

private fun createTempPhotoFile(context: Context): Uri? = try {
    val tempDir = File(context.cacheDir, "camera_temp").also { it.mkdirs() }
    val tempFile = File.createTempFile("photo_", ".jpg", tempDir)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
} catch (e: Exception) { null }
