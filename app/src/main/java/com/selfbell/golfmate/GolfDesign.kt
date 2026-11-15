package com.selfbell.golfmate

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val GolfColors = darkColorScheme(
    background = Color(0xFF101913),
    surface = Color(0xFF1E2D24),
    primary = Color(0xFF8CE577),
    secondary = Color(0xFFEAF8E4),
    onPrimary = Color(0xFF101913),
    secondaryContainer = Color(0xFF26382D),
    onSecondaryContainer = Color(0xFFF1F7F2),
    onBackground = Color(0xFFF1F7F2),
    onSurface = Color(0xFFF1F7F2),
    onSurfaceVariant = Color(0xFFB8C7BC)
)

internal val GolfCardShape = RoundedCornerShape(22.dp)
internal val GolfTypography = Typography().let {
    it.copy(
        headlineMedium = it.headlineMedium.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
        headlineLarge = it.headlineLarge.copy(fontSize = 38.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold),
        displaySmall = it.displaySmall.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        titleMedium = it.titleMedium.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = it.bodyLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
        bodyMedium = it.bodyMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
        bodySmall = it.bodySmall.copy(fontSize = 16.sp, lineHeight = 22.sp)
    )
}

@androidx.compose.runtime.Composable
internal fun GolfHeader(title: String, status: String, onHome: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
        if (onHome != null) {
            OutlinedButton(onClick = onHome, modifier = Modifier.heightIn(min = 72.dp)) {
                Text("홈으로", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(status, color = GolfColors.primary, style = MaterialTheme.typography.titleMedium)
        }
    }
    HorizontalDivider(color = GolfColors.secondaryContainer)
}

@androidx.compose.runtime.Composable
internal fun GolfPanel(modifier: Modifier = Modifier, content: @androidx.compose.runtime.Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = GolfCardShape, color = GolfColors.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically), content = content)
    }
}
