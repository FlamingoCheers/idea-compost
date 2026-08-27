package com.ideacompost.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 衬线标题走系统 Serif（CJK 设备回退 Noto Serif CJK），正式版打包 Noto Serif SC
val SerifDisplay = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
    fontSize = 23.sp, lineHeight = 33.sp
)
val SerifTitle = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
    fontSize = 21.sp, lineHeight = 30.sp
)
val SerifSection = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp, lineHeight = 22.sp
)
val SerifBody = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp, lineHeight = 23.sp
)
val SansBody = TextStyle(
    fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp, lineHeight = 23.sp
)
val SansNote = TextStyle(
    fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
    fontSize = 12.sp, lineHeight = 19.sp
)
val SansTiny = TextStyle(
    fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
    fontSize = 10.5.sp, lineHeight = 15.sp
)
val Wordmark = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
    fontSize = 17.sp, lineHeight = 22.sp
)

val AppTypography = Typography()
