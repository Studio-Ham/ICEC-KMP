package com.studioham.icec.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.designsystem.ZoomState
import com.studioham.icec.designsystem.fitRect
import com.studioham.icec.designsystem.isSideBySide
import com.studioham.icec.designsystem.rememberZoomState
import com.studioham.icec.designsystem.zoomable
import com.studioham.icec.feature.editor.resources.Res
import com.studioham.icec.feature.editor.resources.back
import com.studioham.icec.feature.editor.resources.basic_edit_crop
import com.studioham.icec.feature.editor.resources.basic_edit_crop_area
import com.studioham.icec.feature.editor.resources.basic_edit_done
import com.studioham.icec.feature.editor.resources.basic_edit_exit_body
import com.studioham.icec.feature.editor.resources.basic_edit_flip
import com.studioham.icec.feature.editor.resources.basic_edit_ratio
import com.studioham.icec.feature.editor.resources.basic_edit_ratio_free
import com.studioham.icec.feature.editor.resources.basic_edit_rotate
import com.studioham.icec.feature.editor.resources.basic_edit_title
import com.studioham.icec.feature.editor.resources.ic_back
import com.studioham.icec.feature.editor.resources.ic_crop
import com.studioham.icec.feature.editor.resources.ic_flip
import com.studioham.icec.feature.editor.resources.ic_rotate
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/** [onExit] leaves basic edit (D1: back to the tool palette). [onSaved] gets the new photo's id. */
@Composable
fun BasicEditScreen(
    viewModel: BasicEditViewModel,
    onExit: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = viewModel::onBack)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect {
            when (it) {
                is BasicEditEffect.Saved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSaved(it.photoId)
                }

                BasicEditEffect.Exit -> {
                    onExit()
                }
            }
        }
    }
    if (state.saveError != null) {
        LaunchedEffect(state.saveError) {
            delay(4.seconds)
            viewModel.onSaveErrorDismiss()
        }
    }
    BasicEditContent(
        state,
        BasicEditActions(
            viewModel::onRatioClick,
            viewModel::onCropChange,
            viewModel::onRotateClick,
            viewModel::onFlipClick,
            viewModel::onBack,
            viewModel::onKeepEditing,
            onExit,
            viewModel::onDoneClick,
            viewModel::onSaveErrorAction,
        ),
        modifier,
    )
}

internal class BasicEditActions(
    val onRatioClick: (CropRatio) -> Unit = {},
    val onCropChange: (Rect) -> Unit = {},
    val onRotateClick: () -> Unit = {},
    val onFlipClick: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onKeepEditing: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onDoneClick: () -> Unit = {},
    val onSaveErrorAction: () -> Unit = {},
)

/** Figma `[탐색] 기본 편집 · 자르기 4:3 예시` (623:123): photo, crop ratios, tools and `편집 완료`. */
@Composable
internal fun BasicEditContent(
    state: BasicEditState,
    actions: BasicEditActions,
    modifier: Modifier = Modifier,
) {
    val twoPane = isSideBySide()
    // Hoisted so zoom survives switching between the one- and two-pane trees (rotation, window resize).
    val zoom = rememberZoomState()
    Box(modifier.fillMaxSize().background(Color.Black)) {
        // ponytail: Modifier.blur needs Android 12+; older Android shows the saving overlay without blur.
        Column(
            Modifier.fillMaxSize().then(if (state.saving) Modifier.blur(4.dp) else Modifier).windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            BackTopBar(Res.string.basic_edit_title, actions.onBack)
            if (twoPane) {
                // Like the mosaic editor (#34): photo on the left, controls on the right.
                Row(Modifier.weight(1f)) {
                    CropPhoto(state, zoom, actions.onCropChange, Modifier.weight(1f))
                    Column(Modifier.width(390.dp).fillMaxHeight(), Arrangement.Bottom) { Controls(state, actions) }
                }
            } else {
                CropPhoto(state, zoom, actions.onCropChange, Modifier.weight(1f))
                Controls(state, actions)
            }
        }
        state.saveError?.let { SaveErrorSnackbar(it, actions.onSaveErrorAction, Modifier.align(Alignment.BottomCenter)) }
        if (state.exitDialog) ExitDialog(actions.onExit, actions.onKeepEditing, Res.string.basic_edit_exit_body)
        if (state.saving) Saving()
    }
}

/** Figma `TopAppBar / Back Only` with a centered title; shared by the tool palette and basic edit. */
@Composable
internal fun BackTopBar(
    title: StringResource,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Image(
            painterResource(Res.drawable.ic_back),
            stringResource(Res.string.back),
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(12.dp),
        )
        BasicText(
            stringResource(title),
            Modifier.align(Alignment.Center).padding(horizontal = 58.dp).semantics { heading() },
            style = TextStyle(Color.White, 18.sp, FontWeight.Bold, fontFamily = Pretendard(), textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun Controls(
    state: BasicEditState,
    actions: BasicEditActions,
) {
    RatioChips(state.edit.ratio, actions.onRatioClick)
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Divider),
    )
    Tools(actions.onRotateClick, actions.onFlipClick)
    SaveButton(enabled = true, actions.onDoneClick, Res.string.basic_edit_done)
}

/**
 * The turned, mirrored photo with the crop box: dimmed outside, thirds inside, corners to resize, drag inside to move.
 * Pinch zoom and pan follow the mosaic editor ([zoom]): when zoomed, one finger pans unless it grabs a crop corner. Drawn through [orient], the same transform the save uses.
 */
@Composable
private fun CropPhoto(
    state: BasicEditState,
    zoom: ZoomState,
    onCropChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = state.image
    // Corners at the screen side would start Android's edge back gesture; keep them out of that zone (at least 16dp).
    val inset = WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal).union(WindowInsets(16.dp, 16.dp, 16.dp, 16.dp))
    Box(modifier.fillMaxSize().windowInsetsPadding(inset), contentAlignment = Alignment.Center) {
        if (image == null) return@Box
        val turned = turnedSize(image.width, image.height, state.edit.turns)
        val aspect = turned.width.toFloat() / turned.height
        val photoSize = Size(turned.width.toFloat(), turned.height.toFloat())
        val edit by rememberUpdatedState(state.edit)
        val label = stringResource(Res.string.basic_edit_crop_area)
        Canvas(
            Modifier
                .aspectRatio(aspect)
                .semantics { contentDescription = label }
                .pointerInput(aspect) {
                    var handle: CropHandle? = null
                    detectDragGestures(
                        onDragStart = {
                            val shown = zoom.photoRect(photoSize, size.toSize())
                            // Like the mosaic editor, a zoomed photo pans with one finger; corners still resize the crop.
                            handle =
                                edit.crop
                                    .handleAt(it - shown.topLeft, shown.size, 24.dp.toPx())
                                    .takeUnless { grabbed -> zoom.scale > 1f && grabbed == CropHandle.Move }
                        },
                        onDragEnd = { handle = null },
                        onDragCancel = { handle = null },
                    ) { change, drag ->
                        change.consume()
                        val viewport = size.toSize()
                        val grabbed = handle
                        if (grabbed == null) {
                            // The mosaic editor's rules: no pan at 1x, and the photo keeps covering the viewport.
                            zoom.onGesture(change.position, drag, 1f, fitRect(photoSize, viewport), viewport)
                            return@detectDragGestures
                        }
                        val shown = zoom.photoRect(photoSize, viewport)
                        val moved = Offset(drag.x / shown.width, drag.y / shown.height)
                        onCropChange(edit.crop.dragged(grabbed, moved, edit.ratio, aspect, MinCrop.toPx() / shown.height))
                    }
                }
                // After the crop drag so a pinch cancels it; one finger stays with the crop box or the pan above.
                .zoomable(zoom, singleFingerPan = false) { fitRect(photoSize, it) },
        ) {
            val shown = zoom.photoRect(photoSize, size)
            val margin = 3.dp.toPx() // room for the corner marks on the edge
            clipRect(-margin, -margin, size.width + margin, size.height + margin) {
                val photo = RoundRect(shown, CornerRadius(12.dp.toPx()))
                clipPath(Path().apply { addRoundRect(photo) }) {
                    drawIntoCanvas {
                        it.save()
                        it.translate(shown.left, shown.top)
                        it.scale(shown.width / turned.width, shown.height / turned.height)
                        it.orient(image.width, image.height, edit)
                        it.drawImage(image, Offset.Zero, Paint())
                        it.restore()
                    }
                }
                drawCropBox(edit.crop, shown, photo)
            }
        }
    }
}

/** The crop box over the photo drawn at [shown]: dimmed outside, thirds inside, white frame and corner marks. */
private fun DrawScope.drawCropBox(
    crop: Rect,
    shown: Rect,
    photo: RoundRect,
) {
    val box =
        Rect(
            shown.left + crop.left * shown.width,
            shown.top + crop.top * shown.height,
            shown.left + crop.right * shown.width,
            shown.top + crop.bottom * shown.height,
        )
    val outside =
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRoundRect(photo)
            addRect(box)
        }
    drawPath(outside, Color.Black.copy(alpha = 0.6f))
    val thin = 1.dp.toPx()
    for (i in 1..2) {
        val x = box.left + box.width * i / 3
        val y = box.top + box.height * i / 3
        drawLine(Guide, Offset(x, box.top), Offset(x, box.bottom), thin)
        drawLine(Guide, Offset(box.left, y), Offset(box.right, y), thin)
    }
    drawRect(Color.White, box.topLeft, box.size, style = Stroke(1.5.dp.toPx()))
    val arm = 18.dp.toPx()
    val bold = 3.dp.toPx()
    listOf(
        box.topLeft to Offset(1f, 1f),
        box.topRight to Offset(-1f, 1f),
        box.bottomLeft to Offset(1f, -1f),
        box.bottomRight to Offset(-1f, -1f),
    ).forEach { (corner, inward) ->
        drawLine(Color.White, corner, corner + Offset(arm * inward.x, 0f), bold)
        drawLine(Color.White, corner, corner + Offset(0f, arm * inward.y), bold)
    }
}

/** The handle under [point] for a crop box drawn in [size]: a corner within [reach] px wins, then the inside moves it. */
internal fun Rect.handleAt(
    point: Offset,
    size: Size,
    reach: Float,
): CropHandle? {
    val box = Rect(left * size.width, top * size.height, right * size.width, bottom * size.height)
    val corners =
        listOf(
            CropHandle.TopLeft to box.topLeft,
            CropHandle.TopRight to box.topRight,
            CropHandle.BottomLeft to box.bottomLeft,
            CropHandle.BottomRight to box.bottomRight,
        )
    corners
        .filter { (_, corner) -> abs(point.x - corner.x) <= reach && abs(point.y - corner.y) <= reach }
        .minByOrNull { (_, corner) -> (point - corner).getDistanceSquared() }
        ?.let { return it.first }
    return if (box.contains(point)) CropHandle.Move else null
}

/** Figma 623:139: the selected ratio has a lime outline. Each chip keeps a 48dp touch height. */
@Composable
private fun RatioChips(
    selected: CropRatio,
    onClick: (CropRatio) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(), Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        CropRatio.entries.forEach { ratio ->
            val on = ratio == selected
            val label =
                if (ratio == CropRatio.Free) {
                    stringResource(Res.string.basic_edit_ratio_free)
                } else {
                    stringResource(Res.string.basic_edit_ratio, ratio.width, ratio.height)
                }
            val shape = RoundedCornerShape(22.dp)
            // Up to 76dp each (Figma 623:139); narrower windows share the row so all four chips fit.
            Box(
                Modifier
                    .weight(1f)
                    .widthIn(max = 76.dp)
                    .heightIn(min = 48.dp)
                    .selectable(on, role = Role.RadioButton, onClick = { onClick(ratio) }),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(shape)
                        .then(if (on) Modifier.background(ChipFill).border(1.dp, IcecColors.NeonLime, shape) else Modifier)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        label,
                        style =
                            TextStyle(
                                if (on) IcecColors.NeonLime else Color.White,
                                15.sp,
                                if (on) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = Pretendard(),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Figma 623:149. Crop is always active (it owns the ratio chips and the crop box); rotate (D3: counter-clockwise) and
 * flip act at once.
 */
@Composable
private fun Tools(
    onRotateClick: () -> Unit,
    onFlipClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
        // Up to 104dp each (Figma 623:149); narrower windows share the row.
        val each = Modifier.weight(1f).widthIn(max = 104.dp)
        Tool(Res.drawable.ic_crop, Res.string.basic_edit_crop, selected = true, onClick = null, each)
        Tool(Res.drawable.ic_rotate, Res.string.basic_edit_rotate, selected = false, onClick = onRotateClick, each)
        Tool(Res.drawable.ic_flip, Res.string.basic_edit_flip, selected = false, onClick = onFlipClick, each)
    }
}

@Composable
private fun Tool(
    icon: DrawableResource,
    label: StringResource,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick !=
                    null
                ) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier.semantics { this.selected = selected }
                },
            ).padding(vertical = 8.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterHorizontally,
    ) {
        Image(painterResource(icon), null, Modifier.size(24.dp))
        BasicText(
            stringResource(label),
            style =
                TextStyle(
                    if (selected) IcecColors.NeonLime else Color.White,
                    14.sp,
                    if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = Pretendard(),
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

/** Keeps the corners apart enough to grab: two 48dp touch targets fit inside the box. */
private val MinCrop = 64.dp
private val Guide = Color.White.copy(alpha = 0.5f)
private val Divider = Color(0xFF303030)
private val ChipFill = Color(0xFF121212)

@Preview
@Composable
private fun BasicEditPreview() {
    BasicEditContent(BasicEditState(edit = BasicEdit(ratio = CropRatio.Ratio4x3)), BasicEditActions())
}

@Preview
@Composable
private fun BasicEditExitDialogPreview() {
    BasicEditContent(BasicEditState(edit = BasicEdit(turns = 1), exitDialog = true), BasicEditActions())
}

@Preview
@Composable
private fun BasicEditSavingPreview() {
    BasicEditContent(BasicEditState(edit = BasicEdit(flipped = true), saving = true), BasicEditActions())
}
