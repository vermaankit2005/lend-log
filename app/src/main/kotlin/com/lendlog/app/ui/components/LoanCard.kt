package com.lendlog.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lendlog.app.data.db.Loan
import com.lendlog.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoanCard(
    loan: Loan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val threeDaysMs = 3L * 24 * 60 * 60 * 1000
    val isDueSoon = !loan.isReturned && !loan.isOverdue && (loan.returnDate - now) < threeDaysMs

    val accentColor = when {
        loan.isOverdue  -> Danger
        isDueSoon       -> Warning
        loan.isReturned -> N300
        else            -> Ink
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardBg by animateColorAsState(
        targetValue    = if (isPressed) N100 else N0,
        animationSpec  = tween(100, easing = FastOutLinearInEasing),
        label          = "cardBg"
    )

    val titleColor    = if (loan.isReturned) N500 else N800
    val subtitleColor = if (loan.isReturned) N400 else N500

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left status strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            // Thumbnail
            if (loan.photoUri != null) {
                AsyncImage(
                    model              = loan.photoUri,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(N100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint               = N400,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }

            // Text column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text      = loan.itemName,
                        style     = MaterialTheme.typography.titleLarge,
                        color     = titleColor,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        modifier  = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    LoanStatusBadge(
                        isOverdue  = loan.isOverdue,
                        isDueSoon  = isDueSoon,
                        isReturned = loan.isReturned
                    )
                }

                Text(
                    text     = "Lent to ${loan.borrowerName}",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                DueDateRow(loan = loan, isDueSoon = isDueSoon, accentColor = accentColor)

                if (loan.tagList.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        loan.tagList.take(3).forEach { tag -> TagChip(tag = tag) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DueDateRow(loan: Loan, isDueSoon: Boolean, accentColor: androidx.compose.ui.graphics.Color) {
    val now     = System.currentTimeMillis()
    val daysDiff = ((loan.returnDate - now) / (1000L * 60 * 60 * 24)).toInt()
    val absDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(loan.returnDate))

    val relativeText = when {
        loan.isReturned -> null
        loan.isOverdue  -> "Overdue ${-daysDiff} day${if (-daysDiff != 1) "s" else ""}"
        isDueSoon && daysDiff == 0 -> "Due today"
        isDueSoon       -> "Due in ${daysDiff + 1} day${if (daysDiff + 1 != 1) "s" else ""}"
        else            -> null
    }
    val dotColor = if (loan.isReturned) N300 else accentColor
    val textColor = if (loan.isReturned) N400 else accentColor

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        if (relativeText != null) {
            Text(
                text  = relativeText,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = textColor
            )
            Text(
                text  = " · $absDate",
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = N400
            )
        } else {
            Text(
                text  = absDate,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = if (loan.isReturned) N400 else N500
            )
        }
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
        else       -> Triple(InkSoft, Ink, "Active")
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

// Keep old name for callsites in LoanDetailScreen
@Composable
fun StatusBadge(isOverdue: Boolean) = LoanStatusBadge(isOverdue = isOverdue)

@Composable
fun TagChip(tag: String) {
    Surface(color = N100, shape = RoundedCornerShape(999.dp)) {
        Text(
            text     = tag,
            style    = MaterialTheme.typography.labelSmall,
            color    = N700,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
