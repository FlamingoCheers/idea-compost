package com.ideacompost.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.Color

// MVP：亮色为主（阅读纸感）；字体走系统衬线/无衬线回退，后续版本打包 Noto Serif/Sans SC
private val DarkBg = Color(0xFF201D19)
private val DarkCard = Color(0xFF2A2620)
private val DarkText = Color(0xFFE8E4DA)

private val LightScheme = lightColorScheme(
    primary = Clay,
    onPrimary = PaperWarm,
    secondary = Moss,
    onSecondary = PaperWarm,
    tertiary = Amber,
    background = Paper,
    onBackground = Ink,
    surface = PaperWarm,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Clay
)

private val DarkScheme = darkColorScheme(
    primary = Clay,
    secondary = Moss,
    tertiary = Amber,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText
)

val SerifTitle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 30.sp
)

@Composable
fun IdeaCompostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
