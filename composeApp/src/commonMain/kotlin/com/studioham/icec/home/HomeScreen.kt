package com.studioham.icec.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studioham.icec.resources.Res
import com.studioham.icec.resources.app_name
import com.studioham.icec.resources.home_load_photo
import com.studioham.icec.resources.home_tagline
import com.studioham.icec.resources.ic_photo
import com.studioham.icec.resources.logo
import com.studioham.icec.resources.pretendard_bold
import com.studioham.icec.resources.space_mono_bold
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// ponytail: Figma values inline until core/designsystem owns the tokens.
private val NeonLime = Color(0xFFE6FA00)
private val MutedSlate = Color(0xFFA8A8A8)

/** Figma `[승인] 화면 1: 메인 홈` (315:1272). */
@Composable
fun HomeScreen(
    onLoadPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(Res.drawable.logo), stringResource(Res.string.app_name), Modifier.size(160.dp))
            Spacer(Modifier.height(12.dp))
            BasicText(
                stringResource(Res.string.home_tagline),
                style =
                    TextStyle(
                        color = MutedSlate,
                        fontFamily = FontFamily(Font(Res.font.space_mono_bold)),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 1.4.sp,
                        textAlign = TextAlign.Center,
                    ),
            )
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeonLime)
                .clickable(role = Role.Button, onClick = onLoadPhotoClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_photo), null)
            BasicText(
                stringResource(Res.string.home_load_photo),
                style =
                    TextStyle(
                        color = Color.Black,
                        fontFamily = FontFamily(Font(Res.font.pretendard_bold)),
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.16).sp,
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(onLoadPhotoClick = {})
}
