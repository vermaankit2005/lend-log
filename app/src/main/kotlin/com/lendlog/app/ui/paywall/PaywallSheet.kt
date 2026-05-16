package com.lendlog.app.ui.paywall

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lendlog.app.billing.BillingManager
import com.lendlog.app.ui.components.TealGradientButton
import com.lendlog.app.ui.theme.MutedText
import com.lendlog.app.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    onDismiss: () -> Unit,
    onPurchased: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val billingManager = remember { BillingManager(context) }

    LaunchedEffect(Unit) { billingManager.connect() }
    DisposableEffect(Unit) { onDispose { billingManager.disconnect() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AllInclusive,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(48.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
                }
            }

            Text(
                text = "You've reached the limit",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Free accounts can track up to 3 active loans.\nUnlock unlimited loans for a one-time payment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            TealGradientButton(
                text = if (isLoading) "Processing…" else "Unlock for \$2.99",
                onClick = {
                    isLoading = true
                    billingManager.launchBillingFlow(
                        onSuccess = {
                            isLoading = false
                            onPurchased()
                        },
                        onFailure = { isLoading = false }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = {
                    isLoading = true
                    billingManager.restorePurchases(
                        onRestored = {
                            isLoading = false
                            onPurchased()
                        },
                        onFailure = { isLoading = false }
                    )
                }
            ) {
                Text("Restore purchase", color = MutedText)
            }

            TextButton(onClick = onDismiss) {
                Text("Not now", color = MutedText)
            }
        }
    }
}
