package com.studioham.icec.feature.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studioham.icec.designsystem.ContentMaxWidth
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.designsystem.resources.ic_gallery
import com.studioham.icec.designsystem.resources.ic_retry
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.feature.editor.resources.Res
import com.studioham.icec.feature.editor.resources.back
import com.studioham.icec.feature.editor.resources.face_analysis_failed_body
import com.studioham.icec.feature.editor.resources.face_analysis_failed_title
import com.studioham.icec.feature.editor.resources.face_analysis_finding
import com.studioham.icec.feature.editor.resources.face_analysis_no_face_body
import com.studioham.icec.feature.editor.resources.face_analysis_no_face_title
import com.studioham.icec.feature.editor.resources.face_analysis_pick_another
import com.studioham.icec.feature.editor.resources.face_analysis_retry
import com.studioham.icec.feature.editor.resources.face_analysis_timeout_body
import com.studioham.icec.feature.editor.resources.face_analysis_timeout_title
import com.studioham.icec.feature.editor.resources.ic_alert_triangle
import com.studioham.icec.feature.editor.resources.ic_back
import com.studioham.icec.feature.editor.resources.ic_clock
import com.studioham.icec.feature.editor.resources.ic_no_face
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.studioham.icec.designsystem.resources.Res as DesignRes

/** [onPickAnother] returns to the Gallery's previous position, like the back action (UX-038). */
@Composable
fun FaceAnalysisScreen(
    viewModel: FaceAnalysisViewModel,
    onFacesDetected: (List<FaceBounds>) -> Unit,
    onPickAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val background by produceState<ImageBitmap?>(null, viewModel) { value = viewModel.background(BACKGROUND_PX) }
    // Collecting with lifecycle holds a result reached in the background until the screen is visible again (UX-040).
    LaunchedEffect(state) { (state as? FaceAnalysisState.Detected)?.let { onFacesDetected(it.faces) } }
    FaceAnalysisContent(state, background, viewModel::onRetryClick, onPickAnother, modifier)
}

@Composable
internal fun FaceAnalysisContent(
    state: FaceAnalysisState,
    background: ImageBitmap?,
    onRetryClick: () -> Unit,
    onPickAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        when (state) {
            FaceAnalysisState.NoFace -> {
                NoFace(onPickAnother)
            }

            FaceAnalysisState.Failed -> {
                Problem(
                    Res.drawable.ic_alert_triangle,
                    Res.string.face_analysis_failed_title,
                    Res.string.face_analysis_failed_body,
                    onRetryClick,
                    onPickAnother,
                )
            }

            FaceAnalysisState.Timeout -> {
                Problem(
                    Res.drawable.ic_clock,
                    Res.string.face_analysis_timeout_title,
                    Res.string.face_analysis_timeout_body,
                    onRetryClick,
                    onPickAnother,
                )
            }

            FaceAnalysisState.Analyzing, is FaceAnalysisState.Detected -> {
                Analyzing(background)
            }
        }
    }
}

/** Figma `[승인] 화면 3: 얼굴 분석 로딩` (316:2600): grayscale, blurred, dimmed photo under the spinner. */
@Composable
private fun Analyzing(background: ImageBitmap?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // ponytail: Modifier.blur needs Android 12+; older Android shows the dimmed photo unblurred.
        background?.let {
            Image(it, null, Modifier.fillMaxSize().blur(12.dp), contentScale = ContentScale.Crop, colorFilter = Grayscale)
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Spinner()
            BasicText(
                stringResource(Res.string.face_analysis_finding),
                Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style =
                    TextStyle(
                        Color.White,
                        20.sp,
                        FontWeight.Medium,
                        fontFamily = Pretendard(),
                        letterSpacing = (-0.5).sp,
                        lineHeight = 28.sp,
                    ),
            )
        }
    }
}

@Composable
private fun Spinner() {
    val angle by rememberInfiniteTransition()
        .animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart))
    Canvas(Modifier.size(96.dp)) {
        val inset = 1.dp.toPx()
        drawCircle(SpinnerTrack)
        drawCircle(Color.White, radius = 3.dp.toPx())
        rotate(angle) {
            drawArc(
                SpinnerArc,
                200f,
                140f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = size.copy(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(2.dp.toPx()),
            )
        }
    }
}

/** Figma `[승인] 화면 3-Fail: 얼굴 검출 실패` (316:2750), UX-038 NoFaceDetected. */
@Composable
private fun NoFace(onPickAnother: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Box(Modifier.height(56.dp).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
            // 48dp touch target, same 24dp icon position as the 40dp design.
            Image(
                painterResource(Res.drawable.ic_back),
                stringResource(Res.string.back),
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onPickAnother)
                    .padding(12.dp),
            )
        }
        Message(
            Res.drawable.ic_no_face,
            80.dp,
            Res.string.face_analysis_no_face_title,
            Res.string.face_analysis_no_face_body,
            Modifier.weight(1f),
        )
        Footer {
            Button(DesignRes.drawable.ic_gallery, Res.string.face_analysis_pick_another, primary = true, onPickAnother)
        }
    }
}

/** Figma `[승인] 화면 3-Failed` (340:397) and `3-Timeout` (316:2774), UX-038. */
@Composable
private fun Problem(
    icon: DrawableResource,
    title: StringResource,
    body: StringResource,
    onRetryClick: () -> Unit,
    onPickAnother: () -> Unit,
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Message(icon, 40.dp, title, body, Modifier.weight(1f))
        Footer {
            Button(DesignRes.drawable.ic_retry, Res.string.face_analysis_retry, primary = true, onRetryClick)
            Button(DesignRes.drawable.ic_gallery, Res.string.face_analysis_pick_another, primary = false, onPickAnother)
        }
    }
}

@Composable
private fun Message(
    icon: DrawableResource,
    iconBox: Dp,
    title: StringResource,
    body: StringResource,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .wrapContentWidth()
            .widthIn(max = ContentMaxWidth)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(iconBox), contentAlignment = Alignment.Center) { Image(painterResource(icon), null) }
        BasicText(
            stringResource(title),
            Modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() },
            style =
                TextStyle(
                    Color.White,
                    18.sp,
                    FontWeight.Bold,
                    fontFamily = Pretendard(),
                    letterSpacing = (-0.45).sp,
                    lineHeight = 27.sp,
                    textAlign = TextAlign.Center,
                ),
        )
        BasicText(
            stringResource(body),
            style = TextStyle(IcecColors.MutedSlate, 14.sp, fontFamily = Pretendard(), lineHeight = 22.75.sp, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun Footer(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentWidth()
            .widthIn(max = ContentMaxWidth)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}

@Composable
private fun Button(
    icon: DrawableResource,
    label: StringResource,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (primary) IcecColors.NeonLime else SecondaryFill)
            .then(if (primary) Modifier else Modifier.border(1.dp, SecondaryStroke, shape))
            .clickable(role = Role.Button, onClick = onClick),
        Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        Alignment.CenterVertically,
    ) {
        Image(
            painterResource(icon),
            null,
            Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(if (primary) Color.Black else Color.White),
        )
        BasicText(
            stringResource(label),
            style =
                TextStyle(
                    if (primary) Color.Black else Color.White,
                    16.sp,
                    FontWeight.Bold,
                    fontFamily = Pretendard(),
                    lineHeight = 24.sp,
                ),
        )
    }
}

private const val BACKGROUND_PX = 512
private val SpinnerTrack = Color(0x662A2A2A)
private val SpinnerArc = Color(0xFFDBEE00)
private val SecondaryFill = Color(0xFF242424)
private val SecondaryStroke = Color(0xFF3A3A3A)
private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Preview
@Composable
private fun AnalyzingPreview() {
    FaceAnalysisContent(FaceAnalysisState.Analyzing, null, {}, {})
}

@Preview
@Composable
private fun NoFacePreview() {
    FaceAnalysisContent(FaceAnalysisState.NoFace, null, {}, {})
}

@Preview
@Composable
private fun FailedPreview() {
    FaceAnalysisContent(FaceAnalysisState.Failed, null, {}, {})
}

@Preview
@Composable
private fun TimeoutPreview() {
    FaceAnalysisContent(FaceAnalysisState.Timeout, null, {}, {})
}
