package com.studioham.icec.faceanalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.face.FaceDetector
import com.studioham.icec.resources.Res
import com.studioham.icec.resources.back
import com.studioham.icec.resources.face_analysis_placeholder
import org.jetbrains.compose.resources.stringResource

// ponytail: stand-in for the FaceAnalysis feature. It shows the raw detector result so #18 can be checked in the app;
// #19 replaces it with the Analyzing screen.
@Composable
fun FaceAnalysisPlaceholder(
    photoId: String,
    detector: FaceDetector,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result by produceState<String?>(null, photoId) {
        value = runCatching { detector.detect(photoId) }.fold({ "faces=${it.size}" }, { "error=${it.message}" })
    }
    Column(
        modifier.fillMaxSize().background(Color.Black),
        Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        BasicText(
            stringResource(Res.string.face_analysis_placeholder),
            style = TextStyle(IcecColors.MutedSlate, 14.sp, fontFamily = Pretendard()),
        )
        BasicText(result ?: "…", style = TextStyle(IcecColors.MutedSlate, 14.sp, fontFamily = Pretendard()))
        BasicText(
            stringResource(Res.string.back),
            Modifier.clickable(role = Role.Button, onClick = onBack).padding(12.dp),
            style = TextStyle(IcecColors.NeonLime, 14.sp, fontFamily = Pretendard()),
        )
    }
}

@Preview
@Composable
private fun FaceAnalysisPlaceholderPreview() {
    FaceAnalysisPlaceholder("preview", { emptyList() }, onBack = {})
}
