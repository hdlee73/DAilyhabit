package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 새벽 하늘(남색) + 아침 햇살(호박색) 팔레트
private val Light = lightColorScheme(
    primary = Color(0xFF3A3F8F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E0FF),
    onPrimaryContainer = Color(0xFF14185C),
    secondary = Color(0xFFB8702A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7D0),
    onSecondaryContainer = Color(0xFF3D2200),
    tertiary = Color(0xFF7A4F8F),
    tertiaryContainer = Color(0xFFF3E0FA),
    onTertiaryContainer = Color(0xFF2F0D40),
    background = Color(0xFFF8F6F2),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFF8F6F2),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFECE8E1),
    onSurfaceVariant = Color(0xFF5F5B66),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2EFEA),
    surfaceContainerHigh = Color(0xFFECE9E3),
    surfaceContainerHighest = Color(0xFFE6E2DC),
    outline = Color(0xFFB9B4BE),
    outlineVariant = Color(0xFFE2DED8),
    error = Color(0xFFC0392B),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFBFC2FF),
    onPrimary = Color(0xFF1E2370),
    primaryContainer = Color(0xFF353A84),
    onPrimaryContainer = Color(0xFFE1E0FF),
    secondary = Color(0xFFFFB872),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF5E3A10),
    onSecondaryContainer = Color(0xFFFCE7D0),
    tertiary = Color(0xFFE2B8F2),
    tertiaryContainer = Color(0xFF5A3670),
    onTertiaryContainer = Color(0xFFF3E0FA),
    background = Color(0xFF121218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF121218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF2A2932),
    onSurfaceVariant = Color(0xFFC6C2CE),
    surfaceContainerLowest = Color(0xFF0D0D12),
    surfaceContainerLow = Color(0xFF1B1B22),
    surfaceContainer = Color(0xFF1F1F27),
    surfaceContainerHigh = Color(0xFF262630),
    surfaceContainerHighest = Color(0xFF30303A),
    outline = Color(0xFF8F8B99),
    outlineVariant = Color(0xFF3A3944),
)

private val base = Typography()
private val AppTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

/** 복음·기도문용 본문 서체 */
val ScriptureStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 30.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun DailyHabitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** 루틴 색상 */
val RoutinePalette = listOf(
    Color(0xFF5B5FC7), // 남색
    Color(0xFFE07A3F), // 주황
    Color(0xFF2E9E7A), // 초록
    Color(0xFFD9547A), // 장미
    Color(0xFF3A8FD1), // 하늘
    Color(0xFF9B59B6), // 보라
    Color(0xFFC9A227), // 금색
    Color(0xFF6D7A88), // 회청
)

fun routineColor(index: Int): Color = RoutinePalette[index.mod(RoutinePalette.size)]

/** 전례색 */
fun liturgicalColor(day: String): Color = when {
    day.startsWith("(백)") -> Color(0xFFD4AF37)
    day.startsWith("(홍)") -> Color(0xFFC62828)
    day.startsWith("(녹)") -> Color(0xFF2E7D32)
    day.startsWith("(자)") -> Color(0xFF6A1B9A)
    day.startsWith("(장미)") -> Color(0xFFE48FB1)
    day.startsWith("(흑)") -> Color(0xFF37474F)
    else -> Color(0xFFD4AF37)
}
