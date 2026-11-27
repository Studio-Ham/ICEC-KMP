package com.studioham.icec.feature.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.designsystem.hingeSplit
import com.studioham.icec.designsystem.isSideBySide
import com.studioham.icec.designsystem.separatingHinges
import com.studioham.icec.feature.editor.resources.Res
import com.studioham.icec.feature.editor.resources.ic_mosaic
import com.studioham.icec.feature.editor.resources.ic_tool_edit
import com.studioham.icec.feature.editor.resources.palette_background
import com.studioham.icec.feature.editor.resources.photo_edit_title
import com.studioham.icec.feature.editor.resources.photo_edit_tool_edit
import com.studioham.icec.feature.editor.resources.photo_edit_tool_mosaic
import com.studioham.icec.media.PhotoLibrary
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Wiki 기본 사진 편집: the chosen photo with the tool palette. [onEdit] opens basic edit and [onMosaic] the existing face
 * analysis; both start from the original photo. Back returns to the Gallery's previous position (D1).
 */
@Composable
fun PhotoPreviewScreen(
    photoId: String,
    library: PhotoLibrary,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMosaic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(null, photoId) { value = library.thumbnail(photoId, PREVIEW_PX) }
    PhotoPreviewContent(image, separatingHinges(), onBack, onEdit, onMosaic, modifier)
}

/**
 * Figma `[탐색] 사진 미리보기와 도구 팔레트` (623:87). A separating hinge puts the photo and the palette on its two sides;
 * otherwise wide windows put the palette beside the photo and narrow ones below it (Wiki 적응형 배치).
 */
@Composable
internal fun PhotoPreviewContent(
    image: ImageBitmap?,
    hinges: List<Rect>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMosaic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val beside = isSideBySide()
    var area by remember { mutableStateOf(Rect.Zero) }
    Column(modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackTopBar(Res.string.photo_edit_title, onBack)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { area = it.boundsInWindow() }) {
            val split = hingeSplit(area, hinges)
            val paletteHeight = (maxHeight * 0.45f).coerceIn(PaletteMinHeight, PaletteMaxHeight)
            val px = LocalDensity.current
            when {
                split?.vertical == true -> {
                    Row {
                        PreviewPhoto(image, Modifier.width(with(px) { split.first.toDp() }).fillMaxHeight())
                        Spacer(Modifier.width(with(px) { split.gap.toDp() }))
                        ToolPalette(onEdit, onMosaic, Modifier.weight(1f).fillMaxHeight())
                    }
                }

                split != null -> {
                    Column {
                        PreviewPhoto(image, Modifier.height(with(px) { split.first.toDp() }).fillMaxWidth())
                        Spacer(Modifier.height(with(px) { split.gap.toDp() }))
                        ToolPalette(onEdit, onMosaic, Modifier.weight(1f).fillMaxWidth())
                    }
                }

                // A narrow split window under 480dp tall stays one column (Wiki: 좁은 분할 창은 아래).
                beside -> {
                    Row {
                        PreviewPhoto(image, Modifier.weight(1f).fillMaxHeight())
                        ToolPalette(onEdit, onMosaic, Modifier.width(PaletteColumn).fillMaxHeight())
                    }
                }

                else -> {
                    Column {
                        PreviewPhoto(image, Modifier.weight(1f).fillMaxWidth())
                        ToolPalette(
                            onEdit,
                            onMosaic,
                            Modifier.fillMaxWidth().height(paletteHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPhoto(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        if (image == null) return@Box
        val shape = RoundedCornerShape(12.dp)
        Image(
            image,
            null,
            Modifier.aspectRatio(image.width.toFloat() / image.height).clip(shape).border(1.dp, IcecColors.Hairline, shape),
        )
    }
}

/**
 * The palette vector (Figma 623:101) with separate round buttons at its anchors. It scales as one piece, so a hinge never
 * passes between the palette and its buttons; scale 0.75 or more keeps every button at least 48dp and apart.
 */
@Composable
internal fun ToolPalette(
    onEdit: () -> Unit,
    onMosaic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val fit = minOf((maxWidth - 32.dp) / PaletteWidth, (maxHeight - 32.dp) / PaletteHeight)
        if (fit < MIN_SCALE) {
            // Too small for the palette with 48dp buttons (a tiny hinge pane): keep just the two buttons.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolButton(Res.drawable.ic_tool_edit, Res.string.photo_edit_tool_edit, MIN_SCALE, onEdit)
                ToolButton(Res.drawable.ic_mosaic, Res.string.photo_edit_tool_mosaic, MIN_SCALE, onMosaic)
            }
            return@BoxWithConstraints
        }
        val s = fit.coerceAtMost(1.25f)
        Box(Modifier.requiredSize(PaletteWidth * s, PaletteHeight * s)) {
            Image(painterResource(Res.drawable.palette_background), null, Modifier.fillMaxSize())
            ToolButton(Res.drawable.ic_tool_edit, Res.string.photo_edit_tool_edit, s, onEdit, Modifier.offset(42.dp * s, 61.dp * s))
            ToolButton(Res.drawable.ic_mosaic, Res.string.photo_edit_tool_mosaic, s, onMosaic, Modifier.offset(126.dp * s, 60.dp * s))
        }
    }
}

/** Figma 623:103 / 623:109: a 64dp circle and its name, scaled with the palette. */
@Composable
private fun ToolButton(
    icon: DrawableResource,
    label: StringResource,
    s: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.requiredWidth(84.dp * s).semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .requiredSize(64.dp * s)
                .clip(CircleShape)
                .background(ButtonFill)
                .border((1.2f * s).dp, ButtonStroke, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(icon), null, Modifier.size(24.dp * s))
        }
        BasicText(
            stringResource(label),
            Modifier.padding(top = 9.dp * s),
            style = TextStyle(Color.White, (16 * s).sp, FontWeight.SemiBold, fontFamily = Pretendard(), textAlign = TextAlign.Center),
        )
    }
}

private const val PREVIEW_PX = 2048
private val PaletteWidth = 358.dp
private val PaletteHeight = 222.dp
private val PaletteColumn = 390.dp

/** At this scale the buttons are 48dp and still apart. */
private const val MIN_SCALE = 0.75f
private val PaletteMinHeight: Dp = PaletteHeight * 0.75f + 32.dp
private val PaletteMaxHeight: Dp = PaletteHeight + 32.dp
private val ButtonFill = Color(0xFF2C2C2C)
private val ButtonStroke = Color(0xFF737373)

@Preview
@Composable
private fun PhotoPreviewPreview() {
    PhotoPreviewContent(null, emptyList(), {}, {}, {})
}

@Preview
@Composable
private fun ToolPalettePreview() {
    ToolPalette({}, {}, Modifier.size(390.dp, 254.dp))
}
