package com.studioham.icec.feature.result

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.designsystem.ZoomState
import com.studioham.icec.designsystem.fitRect
import com.studioham.icec.designsystem.isTwoPane
import com.studioham.icec.designsystem.rememberZoomState
import com.studioham.icec.designsystem.zoomable
import com.studioham.icec.feature.result.resources.Res
import com.studioham.icec.feature.result.resources.ic_edit
import com.studioham.icec.feature.result.resources.ic_gallery
import com.studioham.icec.feature.result.resources.ic_home
import com.studioham.icec.feature.result.resources.ic_success
import com.studioham.icec.feature.result.resources.result_edit_again
import com.studioham.icec.feature.result.resources.result_home
import com.studioham.icec.feature.result.resources.result_location
import com.studioham.icec.feature.result.resources.result_other_photo
import com.studioham.icec.feature.result.resources.result_title
import com.studioham.icec.media.PhotoLibrary
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Wiki Result UX: shows the photo actually saved ([savedPhotoId]), not the editor preview. Consumes system back and stays on Result. */
@Composable
fun ResultScreen(
    savedPhotoId: String,
    library: PhotoLibrary,
    onEditAgain: () -> Unit,
    onOtherPhoto: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(null, savedPhotoId) { value = library.thumbnail(savedPhotoId, IMAGE_PX) }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = {})
    ResultContent(image, onEditAgain, onOtherPhoto, onHome, modifier)
}

/** Figma `[승인] 화면 5: 저장 완료` (81:3156). */
@Composable
internal fun ResultContent(
    image: ImageBitmap?,
    onEditAgain: () -> Unit,
    onOtherPhoto: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Hoisted so zoom survives switching between the one- and two-pane trees (rotation, window resize).
    val zoom = rememberZoomState()
    if (isTwoPane()) {
        // #34: saved photo on the left, completion message and actions on the right.
        Row(
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                image?.let { ZoomableImage(it, zoom) }
            }
            // Scrolls only when a landscape phone is too short for the whole panel.
            Box(Modifier.width(340.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Header()
                    Actions(onEditAgain, onOtherPhoto, onHome)
                }
            }
        }
        return
    }
    Column(
        modifier
            .fillMaxSize()
            .background(
                Color.Black,
            ).windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 32.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header()
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            image?.let { ZoomableImage(it, zoom) }
        }
        Actions(onEditAgain, onOtherPhoto, onHome)
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(16.dp, 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(Res.drawable.ic_success), null, Modifier.padding(bottom = 16.dp))
        BasicText(
            stringResource(Res.string.result_title),
            Modifier.semantics { heading() },
            style =
                TextStyle(
                    Color.White,
                    22.sp,
                    FontWeight.Bold,
                    fontFamily = Pretendard(),
                    letterSpacing = (-0.55).sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                ),
        )
        BasicText(
            stringResource(Res.string.result_location),
            Modifier.padding(top = 6.dp),
            style = TextStyle(IcecColors.MutedSlate, 13.sp, fontFamily = Pretendard()),
        )
    }
}

@Composable
private fun Actions(
    onEditAgain: () -> Unit,
    onOtherPhoto: () -> Unit,
    onHome: () -> Unit,
) {
    Column(
        Modifier.widthIn(max = 384.dp).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        Arrangement.spacedBy(12.dp),
    ) {
        Row(
            // 48dp touch target; the 4dp bottom gap is absorbed into the taller row.
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onEditAgain)
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp),
            Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_edit), null)
            BasicText(
                stringResource(Res.string.result_edit_again),
                style = TextStyle(IcecColors.MutedSlate, 14.sp, FontWeight.Medium, fontFamily = Pretendard(), lineHeight = 20.sp),
            )
        }
        Button(
            Res.drawable.ic_gallery,
            Res.string.result_other_photo,
            IcecColors.NeonLime,
            TextStyle(Color.Black, 16.sp, FontWeight.Bold, lineHeight = 20.sp),
            onOtherPhoto,
        )
        Button(
            Res.drawable.ic_home,
            Res.string.result_home,
            HomeFill,
            TextStyle(Color.White, 18.sp, FontWeight.Bold, letterSpacing = (-0.18).sp, lineHeight = 26.sp),
            onHome,
        )
    }
}

@Composable
private fun Button(
    icon: DrawableResource,
    label: StringResource,
    fill: Color,
    style: TextStyle,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .clickable(role = Role.Button, onClick = onClick),
        Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), null)
        BasicText(stringResource(label), style = style.copy(fontFamily = Pretendard()))
    }
}

/** Up to 4x pinch zoom and pan, sharing the Editor's transform rules (#25). */
@Composable
private fun ZoomableImage(
    image: ImageBitmap,
    zoom: ZoomState,
) {
    val imageSize = Size(image.width.toFloat(), image.height.toFloat())
    val shape = RoundedCornerShape(12.dp)
    Canvas(
        Modifier
            .widthIn(max = 384.dp)
            .aspectRatio(imageSize.width / imageSize.height)
            .clip(shape)
            .border(1.dp, IcecColors.Hairline, shape)
            .zoomable(zoom) { fitRect(imageSize, it) },
    ) {
        val shown = zoom.transform(fitRect(imageSize, size), size)
        drawImage(
            image,
            dstOffset = IntOffset(shown.left.roundToInt(), shown.top.roundToInt()),
            dstSize = IntSize(shown.width.roundToInt(), shown.height.roundToInt()),
        )
    }
}

private const val IMAGE_PX = 2048
private val HomeFill = Color(0xFF2A2A2A)

@Preview
@Composable
private fun ResultPreview() {
    ResultContent(null, {}, {}, {})
}
