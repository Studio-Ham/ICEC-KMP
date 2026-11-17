package com.studioham.icec.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.media.Photo
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.resources.Res
import com.studioham.icec.resources.result_edit_again
import com.studioham.icec.resources.result_saved
import org.jetbrains.compose.resources.stringResource

// ponytail: stand-in that shows the saved photo so #26 can be checked end to end. #28 replaces it with
// Figma 81:3156 (zoom, other photo, home).
@Composable
fun ResultPlaceholder(
    savedPhotoId: String,
    library: PhotoLibrary,
    onEditAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(null, savedPhotoId) { value = library.thumbnail(savedPhotoId, 2048) }
    Column(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        Arrangement.spacedBy(16.dp),
        Alignment.CenterHorizontally,
    ) {
        BasicText(
            stringResource(Res.string.result_saved),
            style = TextStyle(Color.White, 18.sp, FontWeight.Bold, fontFamily = Pretendard()),
        )
        image?.let { Image(it, null, Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit) }
        BasicText(
            stringResource(Res.string.result_edit_again),
            Modifier.clickable(role = Role.Button, onClick = onEditAgain).padding(12.dp),
            style = TextStyle(IcecColors.NeonLime, 14.sp, fontFamily = Pretendard()),
        )
    }
}

@Preview
@Composable
private fun ResultPlaceholderPreview() {
    ResultPlaceholder("preview", NoPhotos, onEditAgain = {})
}

private object NoPhotos : PhotoLibrary {
    override suspend fun photos() = emptyList<Photo>()

    override suspend fun isReadable(id: String) = false

    override suspend fun thumbnail(
        id: String,
        sizePx: Int,
    ) = null

    override suspend fun original(id: String) = error("preview")

    override suspend fun save(image: ImageBitmap) = error("preview")
}
