package com.studioham.icec.feature.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.feature.editor.resources.Res
import com.studioham.icec.feature.editor.resources.back
import com.studioham.icec.feature.editor.resources.editor_action_retry
import com.studioham.icec.feature.editor.resources.editor_action_settings
import com.studioham.icec.feature.editor.resources.editor_clear_all
import com.studioham.icec.feature.editor.resources.editor_counter
import com.studioham.icec.feature.editor.resources.editor_detected_faces
import com.studioham.icec.feature.editor.resources.editor_error_failed
import com.studioham.icec.feature.editor.resources.editor_error_no_space
import com.studioham.icec.feature.editor.resources.editor_error_permission
import com.studioham.icec.feature.editor.resources.editor_exit_body
import com.studioham.icec.feature.editor.resources.editor_exit_leave
import com.studioham.icec.feature.editor.resources.editor_exit_stay
import com.studioham.icec.feature.editor.resources.editor_exit_title
import com.studioham.icec.feature.editor.resources.editor_face
import com.studioham.icec.feature.editor.resources.editor_save
import com.studioham.icec.feature.editor.resources.editor_saving_body
import com.studioham.icec.feature.editor.resources.editor_saving_title
import com.studioham.icec.feature.editor.resources.editor_select_all
import com.studioham.icec.feature.editor.resources.ic_arrow_back
import com.studioham.icec.feature.editor.resources.ic_check
import com.studioham.icec.feature.editor.resources.ic_error
import com.studioham.icec.feature.editor.resources.ic_lock
import com.studioham.icec.feature.editor.resources.ic_mosaic
import com.studioham.icec.feature.editor.resources.ic_person
import com.studioham.icec.feature.editor.resources.ic_retry_small
import com.studioham.icec.feature.editor.resources.ic_warning
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** [onExit] leaves the editor after the user confirms (`나가기`). [onSaved] gets the new photo's id. */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onExit: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = viewModel::onBack)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { if (it is EditorEffect.Saved) onSaved(it.photoId) }
    }
    if (state.saveError != null) {
        LaunchedEffect(state.saveError) {
            delay(4.seconds)
            viewModel.onSaveErrorDismiss()
        }
    }
    EditorContent(
        state,
        EditorActions(
            viewModel::onFaceClick,
            viewModel::onSelectAllClick,
            viewModel::onBack,
            viewModel::onKeepEditing,
            onExit,
            viewModel::onSaveClick,
            viewModel::onSaveErrorAction,
        ),
        modifier,
    )
}

internal class EditorActions(
    val onFaceClick: (Int) -> Unit = {},
    val onSelectAllClick: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onKeepEditing: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
    val onSaveErrorAction: () -> Unit = {},
)

/** Figma `[승인] 화면 4: 모자이크 편집 코어` (316:3606), 0 selected (316:4197), exit dialog (316:4294). */
@Composable
internal fun EditorContent(
    state: EditorState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        // ponytail: Modifier.blur needs Android 12+; older Android shows the saving overlay without blur.
        Column(
            Modifier.fillMaxSize().then(if (state.saving) Modifier.blur(4.dp) else Modifier).windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            TopBar(state.faces.size, state.selected.size, actions.onBack)
            Photo(state, actions.onFaceClick, Modifier.weight(1f).padding(16.dp))
            FaceList(state, actions.onFaceClick, actions.onSelectAllClick)
            SaveButton(enabled = state.canSave, actions.onSaveClick)
        }
        state.saveError?.let { SaveErrorSnackbar(it, actions.onSaveErrorAction, Modifier.align(Alignment.BottomCenter)) }
        if (state.exitDialog) ExitDialog(actions.onExit, actions.onKeepEditing)
        if (state.saving) Saving()
    }
}

@Composable
private fun TopBar(
    detected: Int,
    selected: Int,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(57.dp)
            .background(BarFill)
            .padding(bottom = 1.dp),
    ) {
        Image(
            painterResource(Res.drawable.ic_arrow_back),
            stringResource(Res.string.back),
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(12.dp),
        )
        val counter = stringResource(Res.string.editor_counter, detected, selected)
        Row(
            Modifier.align(Alignment.Center).semantics(mergeDescendants = true) { contentDescription = counter },
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically,
        ) {
            Count(Res.drawable.ic_person, detected, Color.White)
            BasicText("/", style = CounterStyle.copy(color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Normal))
            Count(Res.drawable.ic_mosaic, selected, IcecColors.NeonLime)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .offset(y = 1.dp)
                .background(IcecColors.Hairline),
        )
    }
}

@Composable
private fun Count(
    icon: DrawableResource,
    value: Int,
    color: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null)
        BasicText("$value", style = CounterStyle.copy(color = color))
    }
}

/** The photo fitted in the viewport with face guides. Selected faces show the mosaic preview immediately. */
@Composable
private fun Photo(
    state: EditorState,
    onFaceClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = state.image
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(ViewportFill)
            .border(1.dp, IcecColors.Hairline, shape),
    ) {
        if (image == null) return@Box
        val pixels = remember(image, state.faces) { state.faces.map { it.toPixels(image.width, image.height) } }
        val blocks = remember(image, pixels) { pixels.map { image.mosaicBlocks(it) } }
        val check = painterResource(Res.drawable.ic_check)
        Canvas(
            Modifier.fillMaxSize().pointerInput(pixels, image) {
                detectTapGestures { tap ->
                    val fit = fitRect(IntSize(image.width, image.height), Size(size.width.toFloat(), size.height.toFloat()))
                    // Smallest box wins when boxes overlap, so a small face inside a big one stays reachable.
                    pixels.indices
                        .filter { fit.map(pixels[it], image.width).contains(tap) }
                        .minByOrNull { pixels[it].width * pixels[it].height }
                        ?.let(onFaceClick)
                }
            },
        ) {
            val fit = fitRect(IntSize(image.width, image.height), size)
            drawImage(
                image,
                dstOffset = IntOffset(fit.left.toInt(), fit.top.toInt()),
                dstSize = IntSize(fit.width.toInt(), fit.height.toInt()),
            )
            pixels.forEachIndexed { index, face ->
                val box = fit.map(face, image.width)
                val radius = CornerRadius(6.dp.toPx())
                if (index in state.selected) {
                    drawImage(
                        blocks[index],
                        dstOffset = IntOffset(box.left.toInt(), box.top.toInt()),
                        dstSize = IntSize(box.width.toInt(), box.height.toInt()),
                        filterQuality = FilterQuality.None,
                    )
                    drawRoundRect(IcecColors.NeonLime, box.topLeft, box.size, radius, Stroke(2.dp.toPx()))
                    val badge = Offset(box.right, box.top)
                    drawCircle(IcecColors.NeonLime, 10.dp.toPx(), badge)
                    translate(badge.x - check.intrinsicSize.width / 2, badge.y - check.intrinsicSize.height / 2) {
                        with(check) { draw(intrinsicSize) }
                    }
                } else {
                    drawRoundRect(GuideBlue, box.topLeft, box.size, radius, Stroke(1.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun FaceList(
    state: EditorState,
    onFaceClick: (Int) -> Unit,
    onSelectAllClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            BasicText(
                stringResource(Res.string.editor_detected_faces),
                style =
                    TextStyle(
                        Color.White.copy(alpha = 0.9f),
                        12.sp,
                        FontWeight.Medium,
                        fontFamily = Pretendard(),
                        letterSpacing = 0.3.sp,
                        lineHeight = 20.sp,
                    ),
            )
            SelectAllChip(clear = state.selected.isNotEmpty(), onSelectAllClick)
        }
        val image = state.image
        LazyRow(
            Modifier.fillMaxWidth().height(108.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(state.faces) { index, face ->
                FaceItem(index, face, image, selected = index in state.selected) { onFaceClick(index) }
            }
        }
    }
}

@Composable
private fun SelectAllChip(
    clear: Boolean,
    onClick: () -> Unit,
) {
    val color = if (clear) IcecColors.AlertRed else IcecColors.NeonLime
    val fill = if (clear) ClearChipFill else SelectChipFill
    BasicText(
        stringResource(if (clear) Res.string.editor_clear_all else Res.string.editor_select_all),
        Modifier
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 5.dp),
        style = TextStyle(color, 12.sp, FontWeight.Bold, fontFamily = Pretendard(), lineHeight = 12.sp),
    )
}

/** UX-041: each item shows the face cropped from the photo. */
@Composable
private fun FaceItem(
    index: Int,
    face: FaceBounds,
    image: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(Res.string.editor_face, index + 1)
    Box(Modifier.size(84.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(ItemFill)
                .border(if (selected) 2.dp else 1.dp, if (selected) IcecColors.NeonLime else GuideBlue, shape)
                .semantics {
                    contentDescription = label
                    this.selected = selected
                }.clickable(role = Role.Checkbox, onClick = onClick),
        ) {
            if (image != null) {
                val crop = remember(image, face) { faceCrop(face.toPixels(image.width, image.height), image.width, image.height) }
                Canvas(Modifier.fillMaxSize().padding(if (selected) 2.dp else 1.dp).clip(RoundedCornerShape(14.dp))) {
                    drawImage(
                        image,
                        IntOffset(crop.left, crop.top),
                        IntSize(crop.width, crop.height),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    )
                }
            }
        }
        if (selected) CheckBadge(Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp))
    }
}

@Composable
private fun CheckBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(20.dp).clip(CircleShape).background(IcecColors.NeonLime), contentAlignment = Alignment.Center) {
        Image(painterResource(Res.drawable.ic_check), null)
    }
}

/** UX-039: disabled with no selection. */
@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(IcecColors.NeonLime)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            stringResource(Res.string.editor_save),
            style = TextStyle(Color.Black, 16.sp, FontWeight.Bold, fontFamily = Pretendard(), lineHeight = 24.sp),
        )
    }
}

/** Figma `[승인] 화면 4-Dialog: 편집 중 이탈 확인` (316:4294). */
@Composable
private fun BoxScope.ExitDialog(
    onLeave: () -> Unit,
    onStay: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .pointerInput(Unit) {
                detectTapGestures { }
            }.padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(
                    max = 342.dp,
                ).fillMaxWidth()
                .clip(shape)
                .background(ViewportFill)
                .border(1.dp, IcecColors.Hairline, shape)
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(Res.drawable.ic_warning), null)
            BasicText(
                stringResource(Res.string.editor_exit_title),
                Modifier.padding(top = 16.dp, bottom = 8.dp),
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
                stringResource(Res.string.editor_exit_body),
                Modifier.padding(bottom = 24.dp),
                style =
                    TextStyle(
                        IcecColors.MutedSlate,
                        14.sp,
                        fontFamily = Pretendard(),
                        lineHeight = 19.6.sp,
                        textAlign = TextAlign.Center,
                    ),
            )
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                DialogButton(
                    stringResource(Res.string.editor_exit_leave),
                    IcecColors.AlertRed,
                    SecondaryFill,
                    SecondaryStroke,
                    onLeave,
                    Modifier.weight(1f),
                )
                DialogButton(
                    stringResource(Res.string.editor_exit_stay),
                    Color.Black,
                    IcecColors.NeonLime,
                    null,
                    onStay,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    text: Color,
    fill: Color,
    stroke: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(fill)
            .then(if (stroke != null) Modifier.border(1.dp, stroke, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = TextStyle(text, 15.sp, FontWeight.Bold, fontFamily = Pretendard(), lineHeight = 22.5.sp))
    }
}

/** Figma `[승인] 화면 4-Dialog: 저장 중 로딩 다이얼로그` (335:2). Blocks every input while saving (UX-014). */
@Composable
private fun BoxScope.Saving() {
    Column(
        Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.7f)).pointerInput(Unit) { detectTapGestures { } },
        Arrangement.Center,
        Alignment.CenterHorizontally,
    ) {
        val angle by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)))
        Canvas(Modifier.size(48.dp)) {
            val stroke = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round)
            val inset = 6.dp.toPx()
            val arc = Size(size.width - 2 * inset, size.height - 2 * inset)
            drawArc(IcecColors.Hairline, 0f, 360f, false, Offset(inset, inset), arc, style = stroke)
            drawArc(IcecColors.NeonLime, angle - 90f, 90f, false, Offset(inset, inset), arc, style = stroke)
        }
        BasicText(
            stringResource(Res.string.editor_saving_title),
            Modifier.padding(top = 16.dp, bottom = 4.dp),
            style = TextStyle(Color.White, 18.sp, FontWeight.Bold, fontFamily = Pretendard(), lineHeight = 25.sp),
        )
        BasicText(
            stringResource(Res.string.editor_saving_body),
            style = TextStyle(IcecColors.MutedSlate, 14.sp, fontFamily = Pretendard(), lineHeight = 23.sp),
        )
    }
}

/** Figma Editor Snackbar cases 2–4 (316:4050, 316:3747, 316:3897). */
@Composable
private fun SaveErrorSnackbar(
    error: SaveError,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (message, action) =
        when (error) {
            SaveError.NoSpace -> Res.string.editor_error_no_space to null
            SaveError.NoPermission -> Res.string.editor_error_permission to (Res.drawable.ic_lock to Res.string.editor_action_settings)
            SaveError.Failed -> Res.string.editor_error_failed to (Res.drawable.ic_retry_small to Res.string.editor_action_retry)
        }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(SnackbarFill)
            .border(1.dp, IcecColors.Hairline, shape)
            .padding(horizontal = 17.dp, vertical = 14.dp),
        Arrangement.spacedBy(10.dp),
        Alignment.CenterVertically,
    ) {
        Image(painterResource(Res.drawable.ic_error), null)
        BasicText(
            stringResource(message),
            Modifier.weight(1f),
            style = TextStyle(Color.White, 14.sp, FontWeight.Medium, fontFamily = Pretendard(), lineHeight = 20.sp),
        )
        action?.let { (icon, label) ->
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onAction).padding(4.dp),
                Arrangement.spacedBy(4.dp),
                Alignment.CenterVertically,
            ) {
                Image(painterResource(icon), null, Modifier.size(12.dp))
                BasicText(
                    stringResource(label),
                    style = TextStyle(IcecColors.NeonLime, 13.sp, FontWeight.Bold, fontFamily = Pretendard(), lineHeight = 13.sp),
                )
            }
        }
    }
}

private val SnackbarFill = Color(0xFF1C1C1C)
private val CounterStyle = TextStyle(Color.White, 15.sp, FontWeight.SemiBold, letterSpacing = (-0.375).sp, lineHeight = 15.sp)
private val BarFill = Color(0xFF1E1E1E)
private val ViewportFill = Color(0xFF161616)
private val ItemFill = Color(0xFF1E1E1E)
private val GuideBlue = Color(0xCC3098FF)
private val ClearChipFill = Color(0xFF2A1515)
private val SelectChipFill = Color(0x1FE6FA00)
private val SecondaryFill = Color(0xFF242424)
private val SecondaryStroke = Color(0xFF3A3A3A)

private val PreviewFaces = listOf(FaceBounds(0.1f, 0.2f, 0.3f, 0.4f), FaceBounds(0.5f, 0.3f, 0.7f, 0.5f))

@Preview
@Composable
private fun EditorSelectedPreview() {
    EditorContent(EditorState(faces = PreviewFaces, selected = setOf(1)), EditorActions())
}

@Preview
@Composable
private fun EditorNoSelectionPreview() {
    EditorContent(EditorState(faces = PreviewFaces), EditorActions())
}

@Preview
@Composable
private fun EditorExitDialogPreview() {
    EditorContent(EditorState(faces = PreviewFaces, exitDialog = true), EditorActions())
}

@Preview
@Composable
private fun EditorSavingPreview() {
    EditorContent(EditorState(faces = PreviewFaces, selected = setOf(0), saving = true), EditorActions())
}

@Preview
@Composable
private fun EditorSaveFailedPreview() {
    EditorContent(EditorState(faces = PreviewFaces, selected = setOf(0), saveError = SaveError.Failed), EditorActions())
}
