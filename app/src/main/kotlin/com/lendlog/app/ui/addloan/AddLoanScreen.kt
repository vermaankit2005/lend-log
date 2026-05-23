package com.lendlog.app.ui.addloan

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddLoanScreen(
    onNavigateBack: () -> Unit,
    onLoanSaved: () -> Unit,
    viewModel: AddLoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        uri?.let { selected ->
            scope.launch {
                val saved = withContext(Dispatchers.IO) { copyUriToAppStorage(context, selected) }
                if (saved != null) viewModel.updatePhotoUri(saved.toString())
            }
        }
    }

    LaunchedEffect(cameraPermission.status.isGranted, pendingCameraLaunch) {
        if (pendingCameraLaunch && cameraPermission.status.isGranted) {
            pendingCameraLaunch = false
            val uri = createPermanentPhotoFile(context)
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
                            tempCameraUri = createPermanentPhotoFile(context)
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
            Column {
                TopAppBar(
                    title = { Text(if (uiState.isEditMode) "Edit Loan" else "New Loan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = N800) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = N800)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = N50)
                )
                HorizontalDivider(color = N200, thickness = 1.dp)
            }
        },
        containerColor = N50
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotoDropzone(
                photoUri = uiState.photoUri,
                onClick = { showPhotoSheet = true }
            )

            FormField(
                value = uiState.itemName,
                onValueChange = viewModel::updateItemName,
                placeholder = "What are you lending?",
                leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = N400, modifier = Modifier.size(20.dp)) }
            )

            FormField(
                value = uiState.borrowerName,
                onValueChange = { viewModel.updateBorrower(it) },
                placeholder = "Borrower's name",
                capitalization = KeyboardCapitalization.Words,
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

            DateField(
                selectedDate     = uiState.lentDate,
                onDateSelected   = viewModel::updateLentDate,
                context          = context,
                label            = "Date lent",
                allowPastDates   = true,
                allowFutureDates = false
            )

            DateField(
                selectedDate   = uiState.returnDate,
                onDateSelected = viewModel::updateReturnDate,
                context        = context,
                label          = "Return by",
                placeholder    = "Pick a return date",
                allowPastDates = uiState.isEditMode
            )
            QuickDateChips(onDateSelected = viewModel::updateReturnDate)

            FormField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                placeholder = "Notes (optional)",
                minLines = 2,
                maxLines = 3,
                leadingIcon = { Icon(Icons.Outlined.Notes, contentDescription = null, tint = N400, modifier = Modifier.size(20.dp)) }
            )

            FormField(
                value = uiState.tags,
                onValueChange = viewModel::updateTags,
                placeholder = "Tags: book, tools, electronics…",
                capitalization = KeyboardCapitalization.None,
                leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null, tint = N400, modifier = Modifier.size(20.dp)) }
            )

            TealGradientButton(
                text = when {
                    uiState.isSaving   -> "Saving…"
                    uiState.isEditMode -> "Save Changes"
                    else               -> "Save Loan"
                },
                onClick = viewModel::saveLoan,
                enabled = uiState.isValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
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
    maxLines: Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences
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
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
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
    AnimatedContent(
        targetState = photoUri,
        transitionSpec = {
            if (targetState != null) {
                (scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    initialScale = 0.85f
                ) + fadeIn()) togetherWith (scaleOut(targetScale = 0.85f) + fadeOut())
            } else {
                fadeIn() togetherWith (scaleOut(targetScale = 0.85f) + fadeOut())
            }
        },
        label = "photoDropzone"
    ) { uri ->
        if (uri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
            ) {
                AsyncImage(
                    model = uri,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(N50, RoundedCornerShape(12.dp))
                    .border(1.dp, N300, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = null,
                    tint = N400,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text("Add a photo", style = MaterialTheme.typography.labelLarge, color = N600)
                    Text("Optional", style = MaterialTheme.typography.bodySmall, color = N400)
                }
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
    context: Context,
    label: String,
    placeholder: String = "Select a date",
    allowPastDates: Boolean = false,
    allowFutureDates: Boolean = true
) {
    val calendar = Calendar.getInstance().apply {
        selectedDate?.let { timeInMillis = it }
    }
    val now = System.currentTimeMillis()

    val displayText = selectedDate?.let { date ->
        val absDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(date))
        val daysUntil = TimeUnit.MILLISECONDS.toDays(date - now)
        val relText = when {
            !allowFutureDates -> when {
                daysUntil == 0L  -> "today"
                daysUntil == -1L -> "yesterday"
                else             -> null
            }
            daysUntil < 0   -> "overdue"
            daysUntil == 0L -> "today"
            daysUntil == 1L -> "tomorrow"
            else            -> "in $daysUntil days"
        }
        if (relText != null) "$absDate · $relText" else absDate
    } ?: ""

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
        ).apply {
            if (!allowPastDates) datePicker.minDate = System.currentTimeMillis()
            if (!allowFutureDates) datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = N0,
                disabledBorderColor = N200,
                disabledLabelColor = N400,
                disabledTextColor = N800,
                disabledLeadingIconColor = N400,
                disabledTrailingIconColor = N400,
                disabledPlaceholderColor = N400
            )
        )
    }
}

@Composable
private fun QuickDateChips(onDateSelected: (Long) -> Unit) {
    val chips = listOf(
        "3 days"  to 3,
        "1 week"  to 7,
        "2 weeks" to 14,
        "1 month" to 30,
    )
    Row(
        modifier              = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (label, days) ->
            val epochMs = System.currentTimeMillis() + days.toLong() * 24 * 60 * 60 * 1000
            SuggestionChip(
                onClick = { onDateSelected(epochMs) },
                label   = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors  = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = BrandSoft,
                    labelColor     = BrandDeep
                )
            )
        }
    }
}

private fun createPermanentPhotoFile(context: Context): Uri? = try {
    val dir = File(context.filesDir, "loan_photos").also { it.mkdirs() }
    val file = File(dir, "photo_${java.util.UUID.randomUUID()}.jpg")
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (e: Exception) { null }

private fun copyUriToAppStorage(context: Context, source: Uri): Uri? = try {
    val dir = File(context.filesDir, "loan_photos").also { it.mkdirs() }
    val dest = File(dir, "photo_${java.util.UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
} catch (e: Exception) { null }
