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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.feature.editor.resources.Res
import com.studioham.icec.feature.editor.resources.face_analysis_failed
import com.studioham.icec.feature.editor.resources.face_analysis_finding
import com.studioham.icec.feature.editor.resources.face_analysis_no_face
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FaceAnalysisScreen(
    viewModel: FaceAnalysisViewModel,
    onFacesDetected: (List<FaceBounds>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val background by produceState<ImageBitmap?>(null, viewModel) { value = viewModel.background(BACKGROUND_PX) }
    // Collecting with lifecycle holds a result reached in the background until the screen is visible again (UX-040).
    LaunchedEffect(state) { (state as? FaceAnalysisState.Detected)?.let { onFacesDetected(it.faces) } }
    FaceAnalysisContent(state, background, modifier)
}

/** Figma `[승인] 화면 3: 얼굴 분석 로딩` (316:2600). */
@Composable
internal fun FaceAnalysisContent(
    state: FaceAnalysisState,
    background: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        // ponytail: Modifier.blur needs Android 12+; older Android shows the dimmed photo unblurred.
        background?.let { Image(it, null, Modifier.fillMaxSize().blur(12.dp), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        when (state) {
            // ponytail: #20 replaces these two with the NoFaceDetected and AnalysisFailed screens.
            FaceAnalysisState.NoFace -> Message(Res.string.face_analysis_no_face)

            FaceAnalysisState.Failed -> Message(Res.string.face_analysis_failed)

            else -> Analyzing()
        }
    }
}

@Composable
private fun Analyzing() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Spinner()
        Message(Res.string.face_analysis_finding)
    }
}

@Composable
private fun Spinner() {
    val angle by rememberInfiniteTransition()
        .animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart))
    Canvas(Modifier.size(96.dp)) {
        drawCircle(SpinnerTrack)
        drawCircle(Color.White, radius = 3.dp.toPx())
        rotate(angle) {
            drawArc(
                SpinnerArc,
                200f,
                140f,
                useCenter = false,
                style = Stroke(2.dp.toPx()),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size =
                    size.copy(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun Message(text: StringResource) {
    BasicText(
        stringResource(text),
        Modifier.padding(horizontal = 16.dp),
        style =
            TextStyle(
                Color.White,
                20.sp,
                FontWeight.Medium,
                fontFamily = Pretendard(),
                letterSpacing = (-0.5).sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
            ),
    )
}

private const val BACKGROUND_PX = 512
private val SpinnerTrack = Color(0x662A2A2A)
private val SpinnerArc = Color(0xFFDBEE00)

@Preview
@Composable
private fun AnalyzingPreview() {
    FaceAnalysisContent(FaceAnalysisState.Analyzing, null)
}

@Preview
@Composable
private fun FailedPreview() {
    FaceAnalysisContent(FaceAnalysisState.Failed, null)
}
