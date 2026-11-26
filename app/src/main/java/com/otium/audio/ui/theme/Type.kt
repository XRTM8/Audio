package com.otium.audio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.otium.audio.R

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.SemiBold),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 14.sp)
)
