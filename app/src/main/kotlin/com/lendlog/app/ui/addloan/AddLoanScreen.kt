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
import com.lendlog.app.ui.theme.BorderColor
import com.lendlog.app.ui.theme.MutedText
import com.lendlog.app.ui.theme.TealPrimary
import com.lendlog.app.util.ContactPickerHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddLoanScreen(
    onNavigateBack: () -> Unit,
    onLoanSaved: () -> Unit,
    viewModel: AddLoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.savedLoanId) {
        if (uiState.savedLoanId != null) onLoanSaved()
    }

    // Camera permission + temp URI
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.updatePhotoUri(tempCameraUri.toString())
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updatePhotoUri(it.toString()) }
    }

    // Contact picker
    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { contactUri ->
            val (name, phone, contactId) = ContactPickerHelper.resolveContact(context, contactUri)
            if (name != null) viewModel.updateBorrower(name, contactId, phone)
        }
    }

    if (uiState.showPaywall) {
        PaywallSheet(
            onDismiss = viewModel::dismissPaywall,
            onPurchased = {
                viewModel.dismissPaywall()
                viewModel.saveLoan()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Loan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo section
            PhotoSection(
                photoUri = uiState.photoUri,
                onTakePhoto = {
                    if (cameraPermission.status.isGranted) {
                        tempCameraUri = createTempPhotoFile(context)
                        tempCameraUri?.let { cameraLauncher.launch(it) }
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                },
                onPickFromGallery = { galleryLauncher.launch("image/*") },
                onRemovePhoto = { viewModel.updatePhotoUri(null) }
            )

            // Item name
            SectionLabel("Item name *")
            OutlinedTextField(
                value = uiState.itemName,
                onValueChange = viewModel::updateItemName,
                placeholder = { Text("What are you lending?") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            SectionLabel("Notes")
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                placeholder = { Text("Any notes (optional)") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Borrower
            SectionLabel("Borrower *")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { contactLauncher.launch(null) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Contacts")
                }
                OutlinedButton(
                    onClick = { viewModel.updateBorrower("") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Manual")
                }
            }
            OutlinedTextField(
                value = uiState.borrowerName,
                onValueChange = { viewModel.updateBorrower(it) },
                placeholder = { Text("Borrower's name") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // Return date
            SectionLabel("Return date *")
            DatePickerField(
                selectedDate = uiState.returnDate,
                onDateSelected = viewModel::updateReturnDate,
                context = context
            )

            // Tags
            SectionLabel("Tags")
            OutlinedTextField(
                value = uiState.tags,
                onValueChange = viewModel::updateTags,
                placeholder = { Text("e.g. book, tools, electronics") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            TealGradientButton(
                text = if (uiState.isSaving) "Saving…" else "Save Loan",
                onClick = viewModel::saveLoan,
                enabled = uiState.isValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PhotoSection(
    photoUri: String?,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Photo")
        if (photoUri != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Loan photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                IconButton(
                    onClick = onRemovePhoto,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove photo", tint = Color.White)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                PhotoButton(
                    icon = Icons.Outlined.CameraAlt,
                    label = "Camera",
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = BorderColor
                )
                PhotoButton(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "Gallery",
                    onClick = onPickFromGallery,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "A photo helps you remember the exact item",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
    }
}

@Composable
private fun PhotoButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = TealPrimary, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TealPrimary)
    }
}

@Composable
private fun DatePickerField(
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit,
    context: Context
) {
    val calendar = Calendar.getInstance()
    val displayText = selectedDate?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Select date"

    val showPicker = {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day, 23, 59, 59)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    Box(modifier = Modifier.fillMaxWidth().clickable { showPicker() }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        )
    }
}

private fun createTempPhotoFile(context: Context): Uri? {
    return try {
        val tempDir = File(context.cacheDir, "camera_temp").also { it.mkdirs() }
        val tempFile = File.createTempFile("photo_", ".jpg", tempDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    } catch (e: Exception) {
        null
    }
}
