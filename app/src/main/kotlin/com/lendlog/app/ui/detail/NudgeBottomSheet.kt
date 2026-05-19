package com.lendlog.app.ui.detail

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.WhatsApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lendlog.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeBottomSheet(
    borrowerName: String,
    borrowerPhone: String,
    itemName: String,
    onDismiss: () -> Unit,
    onWhatsAppSelected: () -> Unit,
    onSmsSelected: () -> Unit
) {
    val context = LocalContext.current
    val isWhatsAppInstalled = remember {
        listOf("com.whatsapp", "com.whatsapp.w4b").any { pkg ->
            try { context.packageManager.getPackageInfo(pkg, 0); true }
            catch (_: PackageManager.NameNotFoundException) { false }
        }
    }

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
                text = "Nudge $borrowerName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = N800
            )

            Surface(
                color = BrandSoft,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"Hey! Just a reminder — you still have my $itemName. Would love to get it back soon 😊\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandDeep,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (isWhatsAppInstalled) {
                NudgeOption(
                    icon = Icons.Outlined.WhatsApp,
                    title = "Send via WhatsApp",
                    subtitle = "Opens WhatsApp with the message ready to send",
                    onClick = { onWhatsAppSelected(); onDismiss() }
                )
            }

            NudgeOption(
                icon = Icons.Outlined.Message,
                title = "Send via SMS",
                subtitle = "Opens your SMS app with the message ready to send",
                onClick = { onSmsSelected(); onDismiss() }
            )
        }
    }
}

@Composable
private fun NudgeOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = N50,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandSoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Brand, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = N800
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = N500)
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = N300,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
