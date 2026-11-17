package com.studioham.icec.feature.gallery

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.feature.gallery.resources.Res
import com.studioham.icec.feature.gallery.resources.gallery_back
import com.studioham.icec.feature.gallery.resources.gallery_permission_allow
import com.studioham.icec.feature.gallery.resources.gallery_permission_body
import com.studioham.icec.feature.gallery.resources.gallery_permission_title
import com.studioham.icec.feature.gallery.resources.gallery_title
import com.studioham.icec.feature.gallery.resources.ic_back
import com.studioham.icec.feature.gallery.resources.ic_chevron
import com.studioham.icec.feature.gallery.resources.ic_gear
import com.studioham.icec.feature.gallery.resources.ic_photo_large
import com.studioham.icec.media.PhotoAccess
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val access by viewModel.access.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    GalleryContent(access, onBack, viewModel::onAllowClick, modifier)
}

@Composable
private fun GalleryContent(
    access: PhotoAccess,
    onBack: () -> Unit,
    onAllowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        TopBar(onBack)
        Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
            when (access) {
                PhotoAccess.Granted, PhotoAccess.Limited -> Unit

                // ponytail: photo grid lands in Gallery ② (#13).
                else -> PermissionRequired(onAllowClick)
            }
        }
    }
}

/** Figma 316:2478 */
@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(IcecColors.StudioSlate)
            .drawBehind { drawLine(IcecColors.Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) },
    ) {
        Box(
            Modifier
                .padding(start = 16.dp)
                .size(40.dp)
                .align(Alignment.CenterStart)
                .clickable(role = Role.Button, onClick = onBack),
            Alignment.Center,
        ) { Image(painterResource(Res.drawable.ic_back), stringResource(Res.string.gallery_back)) }
        BasicText(
            stringResource(Res.string.gallery_title),
            Modifier.align(Alignment.Center),
            TextStyle(
                Color.White,
                15.sp,
                FontWeight.SemiBold,
                fontFamily = Pretendard(),
                letterSpacing = (-0.375).sp,
                lineHeight = 22.5.sp,
            ),
        )
    }
}

/** Figma `[승인] 화면 2-Permission` (316:2477), UX-020~022. */
@Composable
private fun PermissionRequired(onAllowClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(Res.drawable.ic_photo_large), null)
        Spacer(Modifier.height(16.dp))
        BasicText(
            stringResource(Res.string.gallery_permission_title),
            style =
                TextStyle(
                    Color.White,
                    18.sp,
                    FontWeight.Bold,
                    fontFamily = Pretendard(),
                    letterSpacing = (-0.45).sp,
                    lineHeight = 27.sp,
                ),
        )
        BasicText(
            stringResource(Res.string.gallery_permission_body),
            style =
                TextStyle(
                    IcecColors.MutedSlate,
                    14.sp,
                    fontFamily = Pretendard(),
                    lineHeight = 22.75.sp,
                    textAlign = TextAlign.Center,
                ),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onAllowClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            Arrangement.spacedBy(6.dp),
            Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_gear), null)
            BasicText(
                stringResource(Res.string.gallery_permission_allow),
                style = TextStyle(IcecColors.NeonLime, 14.sp, FontWeight.Medium, fontFamily = Pretendard(), lineHeight = 21.sp),
            )
            Image(painterResource(Res.drawable.ic_chevron), null)
        }
    }
}

@Preview
@Composable
private fun PermissionRequiredPreview() {
    GalleryContent(PhotoAccess.NotDetermined, onBack = {}, onAllowClick = {})
}
