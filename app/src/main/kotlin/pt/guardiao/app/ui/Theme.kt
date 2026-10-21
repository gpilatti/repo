package pt.guardiao.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pt.guardiao.core.model.RiskLevel

private val Brand = Color(0xFF1F4E79)

private val Light = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEDEFF2),
    onSurfaceVariant = Color(0xFF2B2B2B),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CC3EA),
    onPrimary = Color(0xFF0B2236),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1C1C1C),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF2A2D31),
    onSurfaceVariant = Color(0xFFE6E6E6),
)

/** Large, high-contrast type. All sizes are in sp, so the system font size setting applies. */
private val SeniorTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 29.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun GuardiaoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = SeniorTypography,
        content = content,
    )
}

/**
 * Risk colours meet WCAG AA contrast with their text colour. Colour is never
 * the only cue: every level also has an icon and a written label.
 */
data class RiskStyle(val container: Color, val content: Color)

fun riskStyle(level: RiskLevel): RiskStyle = when (level) {
    RiskLevel.CRITICAL -> RiskStyle(Color(0xFF9B1C1C), Color.White)
    RiskLevel.HIGH -> RiskStyle(Color(0xFFB45309), Color.White)
    RiskLevel.CAUTION -> RiskStyle(Color(0xFFFFE8A3), Color(0xFF3D2E00))
    RiskLevel.NO_WARNING -> RiskStyle(Color(0xFFE3EDF7), Color(0xFF14324F))
}
