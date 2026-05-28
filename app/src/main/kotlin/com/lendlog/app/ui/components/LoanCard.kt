package com.lendlog.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Shared formatters — safe for single-threaded Compose use (all recomposition on Main thread).
private val FMT_SHORT_DATE = SimpleDateFormat("MMM d", Locale.getDefault())

private val avatarPalette = listOf(
    Color(0xFF0E9AA7), // teal
    Color(0xFF7C3AED), // violet
    Color(0xFF059669), // emerald
    Color(0xFFD97706), // amber
    Color(0xFFDB2777), // pink
    Color(0xFF2563EB), // blue
)

private fun avatarColor(name: String): Color =
    avatarPalette[Math.floorMod(name.trim().lowercase().hashCode(), avatarPalette.size)]

private fun initials(name: String): String =
    name.trim().split(" ").filter { it.isNotEmpty() }
        .take(2).joinToString("") { it[0].uppercaseChar().toString() }

@Composable
fun LoanCard(
    loan: Loan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val threeDaysMs = 3L * 24 * 60 * 60 * 1000
    val isDueSoon = !loan.isReturned && !loan.isOverdue && (loan.returnDate - now) < threeDaysMs

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardBg = when {
        isPressed      -> N100
        loan.isOverdue -> DangerSoft
        isDueSoon      -> WarningSoft
        loan.isReturned -> N50
        else           -> N0
    }
    val animatedBg by animateColorAsState(
        targetValue   = cardBg,
        animationSpec = tween(100, easing = FastOutLinearInEasing),
        label         = "cardBg"
    )

    val borderMod = if (loan.isOverdue) {
        Modifier.border(width = 1.dp, color = Danger.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
    } else {
        Modifier.border(width = 1.dp, color = N100, shape = RoundedCornerShape(12.dp))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(borderMod)
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        // Red left accent strip — only for overdue
        if (loan.isOverdue) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Danger)
                    .align(Alignment.CenterStart)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (loan.isOverdue) 16.dp else 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Borrower avatar
            BorrowerAvatar(name = loan.borrowerName, isReturned = loan.isReturned)

            Spacer(Modifier.width(12.dp))

            // Main content — person first
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Row 1: borrower name + lent date
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = loan.borrowerName,
                        style    = MaterialTheme.typography.titleLarge,
                        color    = if (loan.isReturned) N500 else N800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = FMT_SHORT_DATE.format(Date(loan.lentDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = N400
                    )
                }

                // Row 2: status + item name (inline)
                StatusAndItemLine(loan = loan, isDueSoon = isDueSoon)

                // Row 3: tags
                if (loan.tagList.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        loan.tagList.take(3).forEach { tag -> TagChip(tag = tag) }
                    }
                }
            }

            // Photo thumbnail (right, small)
            if (loan.photoUri != null) {
                Spacer(Modifier.width(10.dp))
                AsyncImage(
                    model              = loan.photoUri,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun BorrowerAvatar(name: String, isReturned: Boolean) {
    val bg = if (isReturned) N300 else avatarColor(name)
    Box(
        modifier         = Modifier
            .size(40.dp)
            .background(bg.copy(alpha = if (isReturned) 1f else 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = initials(name).ifEmpty { "?" },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isReturned) N500 else bg
        )
    }
}

@Composable
private fun StatusAndItemLine(loan: Loan, isDueSoon: Boolean) {
    val now      = System.currentTimeMillis()
    val daysDiff = ((loan.returnDate - now) / (1000L * 60 * 60 * 24)).toInt()

    val (statusText, statusColor) = when {
        loan.isReturned -> "Returned" to N400
        loan.isOverdue  -> "Overdue ${-daysDiff} day${if (-daysDiff != 1) "s" else ""}" to Danger
        isDueSoon && daysDiff == 0 -> "Due today" to Warning
        isDueSoon       -> "Due in $daysDiff day${if (daysDiff != 1) "s" else ""}" to Warning
        else            -> {
            val absDate = FMT_SHORT_DATE.format(Date(loan.returnDate))
            "Due $absDate" to N500
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "overduePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = if (loan.isOverdue) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text     = statusText,
            style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color    = statusColor,
            maxLines = 1,
            modifier = if (loan.isOverdue) Modifier.graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            } else Modifier
        )
        Text(
            text     = " · ${loan.itemName}",
            style    = MaterialTheme.typography.bodySmall,
            color    = if (loan.isReturned) N400 else N500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LoanStatusBadge(
    isOverdue: Boolean,
    isDueSoon: Boolean = false,
    isReturned: Boolean = false
) {
    val (bg, textColor, label) = when {
        isReturned -> Triple(N100, N500, "Returned")
        isOverdue  -> Triple(DangerSoft, Danger, "Overdue")
        isDueSoon  -> Triple(WarningSoft, Warning, "Soon")
        else       -> Triple(BrandSoft, Brand, "Active")
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelMedium,
            color    = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusBadge(isOverdue: Boolean) = LoanStatusBadge(isOverdue = isOverdue)

@Composable
fun TagChip(tag: String) {
    Surface(color = N100, shape = RoundedCornerShape(999.dp)) {
        Text(
            text     = tag,
            style    = MaterialTheme.typography.labelSmall,
            color    = N600,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
