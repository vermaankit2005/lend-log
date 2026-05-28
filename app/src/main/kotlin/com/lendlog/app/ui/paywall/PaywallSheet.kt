package com.lendlog.app.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lendlog.app.ui.components.TealGradientButton
import com.lendlog.app.ui.theme.*

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private val benefits = listOf(
    "Unlimited active loans",
    "Same private, local-only app",
    "One-time payment, no subscription",
    "Yours forever"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    onDismiss: () -> Unit,
    onPurchased: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var showPendingMessage by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Icon
            Box(
                modifier         = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.LockOpen,
                    contentDescription = null,
                    tint               = Brand,
                    modifier           = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "Unlock unlimited loans",
                style     = MaterialTheme.typography.headlineSmall,
                color     = N800,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "You've filled your 3 free slots. Keep tracking everything you lend — no limits, ever.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = N500,
                textAlign = TextAlign.Center,
                modifier  = Modifier.widthIn(max = 300.dp)
            )

            if (showPendingMessage) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text      = "Payment pending — you'll be unlocked once it clears.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.widthIn(max = 300.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Benefit bullets
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                benefits.forEach { benefit ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Check,
                            contentDescription = null,
                            tint               = Brand,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text  = benefit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = N700
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            TealGradientButton(
                text    = if (isLoading) "Processing…" else "Unlock for \$2.99",
                onClick = {
                    val activity = context.findActivity() ?: return@TealGradientButton
                    isLoading = true
                    showPendingMessage = false
                    viewModel.launchBillingFlow(
                        activity  = activity,
                        onSuccess = { isLoading = false; onPurchased() },
                        onFailure = { isLoading = false },
                        onPending = { isLoading = false; showPendingMessage = true }
                    )
                },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        isLoading = true
                        viewModel.restorePurchases(
                            onRestored = { isLoading = false; onPurchased() },
                            onFailure  = { isLoading = false }
                        )
                    }
                ) {
                    Text("Restore purchase", style = MaterialTheme.typography.labelMedium, color = N400)
                }
                Text("·", color = N300, style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = onDismiss) {
                    Text("Maybe later", style = MaterialTheme.typography.labelMedium, color = N400)
                }
            }
        }
    }
}
