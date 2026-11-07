package com.bodyrecord.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Pine = Color(0xFF284B38)
val Ink = Color(0xFF223329)
val Muted = Color(0xFF788377)
val Paper = Color(0xFFF6F7F2)
val Lime = Color(0xFFD9EDB8)
val SoftGreen = Color(0xFFEDF2E5)
val Orange = Color(0xFFB77B50)

@Composable
fun BodyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Pine,onPrimary = Color.White,primaryContainer = Lime,
            secondary = Pine,onSecondary = Color.White,secondaryContainer = Lime,onSecondaryContainer = Ink,
            tertiary = Orange,tertiaryContainer = Color(0xFFF0EDE5),onTertiaryContainer = Ink,
            onPrimaryContainer = Ink,background = Paper,onBackground = Ink,surface = Color.White,
            onSurface = Ink,surfaceVariant = SoftGreen,onSurfaceVariant = Muted,outline = Color(0xFFDCE1D5)),
        shapes = Shapes(small = RoundedCornerShape(12.dp),medium = RoundedCornerShape(20.dp),large = RoundedCornerShape(28.dp)),
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 32.sp,fontWeight = FontWeight.Bold,letterSpacing = (-1).sp),
            headlineMedium = TextStyle(fontSize = 27.sp,fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 20.sp,fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 16.sp,fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp,lineHeight = 25.sp),
            bodyMedium = TextStyle(fontSize = 14.sp,lineHeight = 22.sp),
            labelLarge = TextStyle(fontSize = 14.sp,fontWeight = FontWeight.SemiBold)
        ), content = content
    )
}
val NumberFont = FontFamily.Serif
