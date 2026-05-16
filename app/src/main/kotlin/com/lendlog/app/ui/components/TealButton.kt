package com.lendlog.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lendlog.app.ui.theme.Ink
import com.lendlog.app.ui.theme.LendLogTypography

@Composable
fun TealGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(52.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Ink,
            disabledContainerColor = Ink.copy(alpha = 0.38f),
            contentColor           = Color.White,
            disabledContentColor   = Color.White.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text  = text,
            style = LendLogTypography.labelLarge,
            color = Color.White
        )
    }
}
