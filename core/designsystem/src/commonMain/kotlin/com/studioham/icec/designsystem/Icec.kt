package com.studioham.icec.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.studioham.icec.designsystem.resources.Res
import com.studioham.icec.designsystem.resources.pretendard_bold
import com.studioham.icec.designsystem.resources.pretendard_medium
import com.studioham.icec.designsystem.resources.pretendard_regular
import com.studioham.icec.designsystem.resources.pretendard_semibold
import com.studioham.icec.designsystem.resources.space_mono_bold
import org.jetbrains.compose.resources.Font

/** Colors from Figma ICEC-Design-System (Color page). */
object IcecColors {
    val NeonLime = Color(0xFFE6FA00)
    val MutedSlate = Color(0xFFA8A8A8)
    val StudioSlate = Color(0xFF1E1E1E)
    val Hairline = Color(0xFF333333)
    val IconMuted = Color(0xFF555555)
    val AlertRed = Color(0xFFEF4444)
}

/** Product UI font. */
@Composable
fun Pretendard(): FontFamily =
    FontFamily(
        Font(Res.font.pretendard_regular, FontWeight.Normal),
        Font(Res.font.pretendard_medium, FontWeight.Medium),
        Font(Res.font.pretendard_semibold, FontWeight.SemiBold),
        Font(Res.font.pretendard_bold, FontWeight.Bold),
    )

/** Brand tagline font. */
@Composable
fun SpaceMono(): FontFamily = FontFamily(Font(Res.font.space_mono_bold, FontWeight.Bold))
