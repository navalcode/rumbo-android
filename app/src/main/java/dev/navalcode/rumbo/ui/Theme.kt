package dev.navalcode.rumbo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.navalcode.rumbo.R

val Ink = Color(0xFF202421)
val Muted = Color(0xFF5E625C)
val Paper = Color(0xFFFAF8F3)
val Accent = Color(0xFF9B432F)
val Soft = Color(0xFFF2E3DB)
val Line = Color(0xFFDDD8CD)
val Outline = Color(0xFF85877E)

val ReadingFont = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)
val DisplayFont = FontFamily(
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal,
                 family: FontFamily = ReadingFont) = TextStyle(
    fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = height.sp,
)

@Composable
fun RumboTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent, onPrimary = Color.White,
            primaryContainer = Soft, onPrimaryContainer = Accent,
            secondary = Accent, onSecondary = Color.White,
            secondaryContainer = Soft, onSecondaryContainer = Accent,
            tertiary = Ink, onTertiary = Color.White,
            background = Paper, onBackground = Ink,
            surface = Paper, onSurface = Ink, onSurfaceVariant = Muted,
            outline = Outline, outlineVariant = Line,
            error = Accent, onError = Color.White,
            errorContainer = Soft, onErrorContainer = Ink,
        ),
        typography = Typography(
            displayLarge = text(44, 50, FontWeight.Medium, DisplayFont),
            displayMedium = text(36, 42, FontWeight.Medium, DisplayFont),
            displaySmall = text(32, 38, FontWeight.Medium, DisplayFont),
            headlineLarge = text(32, 38, FontWeight.Medium, DisplayFont),
            headlineMedium = text(28, 34, FontWeight.Medium, DisplayFont),
            headlineSmall = text(24, 30, FontWeight.Medium, DisplayFont),
            titleLarge = text(24, 30, FontWeight.Medium, DisplayFont),
            titleMedium = text(17, 24, FontWeight.SemiBold),
            titleSmall = text(15, 22, FontWeight.SemiBold),
            bodyLarge = text(16, 25),
            bodyMedium = text(14, 21),
            bodySmall = text(12, 18),
            labelLarge = text(14, 20, FontWeight.SemiBold),
            labelMedium = text(12, 18, FontWeight.Medium),
            labelSmall = text(12, 18, FontWeight.SemiBold),
        ),
        content = content,
    )
}
