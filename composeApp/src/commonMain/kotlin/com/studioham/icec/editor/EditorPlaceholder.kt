package com.studioham.icec.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import com.studioham.icec.resources.Res
import com.studioham.icec.resources.back
import com.studioham.icec.resources.editor_placeholder
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// ponytail: stand-in for the Editor so FaceAnalysis has somewhere to go. The Editor Issue replaces it.
@Composable
fun EditorPlaceholder(
    faceCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().background(Color.Black),
        Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        BasicText(
            pluralStringResource(Res.plurals.editor_placeholder, faceCount, faceCount),
            style = TextStyle(IcecColors.MutedSlate, 14.sp, fontFamily = Pretendard()),
        )
        BasicText(
            stringResource(Res.string.back),
            Modifier.clickable(role = Role.Button, onClick = onBack).padding(12.dp),
            style = TextStyle(IcecColors.NeonLime, 14.sp, fontFamily = Pretendard()),
        )
    }
}

@Preview
@Composable
private fun EditorPlaceholderPreview() {
    EditorPlaceholder(3, onBack = {})
}
