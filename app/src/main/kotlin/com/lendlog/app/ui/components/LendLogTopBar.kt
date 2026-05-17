package com.lendlog.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lendlog.app.ui.theme.Inter
import com.lendlog.app.ui.theme.N800

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendLogTopBar(
    showLogo: Boolean = false,
    title: String = "",
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        TopAppBar(
            navigationIcon = {
                if (onNavigateUp != null) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            title = {
                if (showLogo) {
                    Text(
                        text = "LendLog",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = N800
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
    }
}
