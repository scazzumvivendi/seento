package com.scazzumvivendi.seento.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scazzumvivendi.seento.R

private val interFontFamily = FontFamily(
    Font(R.font.lato_regular, weight = FontWeight.Normal),
    Font(R.font.lato_light, weight = FontWeight.Light),
    Font(R.font.lato_bold, weight = FontWeight.Bold),
    Font(R.font.lato_black, weight = FontWeight.Black)
)

// Set of Material typography styles to start with
private val defaultTypography = Typography()

private fun TextStyle.withAppFont() = copy(fontFamily = interFontFamily)

val Typography = defaultTypography.copy(
    displayLarge = defaultTypography.displayLarge.withAppFont(),
    displayMedium = defaultTypography.displayMedium.withAppFont(),
    displaySmall = defaultTypography.displaySmall.withAppFont(),
    headlineLarge = defaultTypography.headlineLarge.withAppFont(),
    headlineMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineSmall = defaultTypography.headlineSmall.withAppFont(),
    titleMedium = defaultTypography.titleMedium.withAppFont(),
    titleSmall = defaultTypography.titleSmall.withAppFont(),
    bodyMedium = defaultTypography.bodyMedium.withAppFont(),
    bodySmall = defaultTypography.bodySmall.withAppFont(),
    labelLarge = defaultTypography.labelLarge.withAppFont(),
    labelMedium = defaultTypography.labelMedium.withAppFont(),
    labelSmall = defaultTypography.labelSmall.withAppFont()
)
