package dev.navalcode.rumbo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF233F33)
val Muted = Color(0xFF657267)
val Paper = Color(0xFFF7F6F2)
val Sage = Color(0xFFE7ECDf)
val Line = Color(0xFFE0E3D9)

@Composable
fun RumboTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink, onPrimary = Color.White, background = Paper,
            surface = Paper, onSurface = Ink, onBackground = Ink, secondaryContainer = Sage,
            onSecondaryContainer = Ink, outlineVariant = Line,
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-1).sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 33.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 25.sp, lineHeight = 29.sp),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            labelSmall = TextStyle(fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
        ),
        content = content,
    )
}
