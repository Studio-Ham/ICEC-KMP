package com.studioham.icec.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPage
import com.studioham.icec.media.PhotoPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BasicEditTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun rotationTurnsCounterClockwiseAndResetsTheCropToTheRatio() {
        val edit = BasicEdit().withRatio(CropRatio.Ratio4x3, 400, 300)
        assertEquals(FullCrop, edit.crop) // a 4:3 photo already is 4:3

        val turned = edit.rotated(400, 300) // now 300 x 400
        assertEquals(1, turned.turns)
        assertClose(Rect(0f, 0.21875f, 1f, 0.78125f), turned.crop) // largest centered 4:3 box (D6)

        val back = (1..3).fold(turned) { it, _ -> it.rotated(400, 300) }
        assertEquals(0, back.turns)
    }

    @Test
    fun rotationAfterMirroringStillTurnsTheScreenCounterClockwise() {
        // Screen = mirror after turns (D6). Turning that counter-clockwise once is mirror after one turn fewer (D3).
        val mirrored = BasicEdit().mirrored()
        assertEquals(3, mirrored.rotated(400, 300).turns)
        assertEquals(
            0,
            mirrored
                .rotated(400, 300)
                .rotated(400, 300)
                .rotated(400, 300)
                .rotated(400, 300)
                .turns,
        )
        assertEquals(1, BasicEdit().rotated(400, 300).turns)
    }

    @Test
    fun ratioBoxPulledBackToTheEdgeStillMoves() {
        // 720 x 1560 passes the 2048px preview unscaled; 4:3 width rounds 1 ULP past 1 at the photo edge.
        val aspect = 720f / 1560
        val start = ratioCrop(CropRatio.Ratio4x3, IntSize(720, 1560))
        val edge =
            start
                .dragged(CropHandle.TopLeft, Offset(0.2f, 0.2f), CropRatio.Ratio4x3, aspect, 0.05f)
                .dragged(CropHandle.TopLeft, Offset(-1f, -1f), CropRatio.Ratio4x3, aspect, 0.05f)
        assertTrue(edge.left >= 0f && edge.top >= 0f && edge.right <= 1f && edge.bottom <= 1f, "$edge")

        val moved = edge.dragged(CropHandle.Move, Offset(0.1f, 0.1f), CropRatio.Ratio4x3, aspect, 0.05f)
        assertTrue(moved.right <= 1f && moved.bottom <= 1f, "$moved")
    }

    @Test
    fun mirroringKeepsTheCroppedContentAndUndoesItself() {
        val edit = BasicEdit(crop = Rect(0.1f, 0.2f, 0.5f, 0.7f))
        assertClose(Rect(0.5f, 0.2f, 0.9f, 0.7f), edit.mirrored().crop)
        assertClose(edit.crop, edit.mirrored().mirrored().crop)
        assertTrue(BasicEdit().isIdentity)
        assertFalse(BasicEdit().mirrored().isIdentity)
        assertTrue(BasicEdit().mirrored().mirrored().isIdentity) // D1: back leaves without asking
    }

    @Test
    fun cornerDragsKeepTheOppositeCornerTheRatioAndTheBounds() {
        assertClose(Rect(0.25f, 0.25f, 1f, 1f), FullCrop.dragged(CropHandle.TopLeft, Offset(0.25f, 0.25f), CropRatio.Free, 4 / 3f, 0.1f))

        // 1:1 on a 400 x 300 photo stays square in pixels.
        val square = ratioCrop(CropRatio.Square, IntSize(400, 300))
        val dragged = square.dragged(CropHandle.BottomRight, Offset(-0.25f, -0.1f), CropRatio.Square, 4 / 3f, 0.1f)
        assertEquals(square.left, dragged.left)
        assertEquals(square.top, dragged.top)
        assertEquals(dragged.width * 400, dragged.height * 300, 0.01f)

        // Dragging past the opposite corner stops at the minimum size.
        assertClose(Rect(0.8f, 0.8f, 1f, 1f), FullCrop.dragged(CropHandle.TopLeft, Offset(2f, 2f), CropRatio.Free, 1f, 0.2f))
        // Moving stays inside the photo.
        assertClose(
            Rect(0.6f, 0f, 1f, 0.4f),
            Rect(0.2f, 0.2f, 0.6f, 0.6f).dragged(CropHandle.Move, Offset(1f, -1f), CropRatio.Free, 1f, 0.1f),
        )
    }

    @Test
    fun previewAndOriginalCropTheSamePixels() {
        val crop = Rect(0.1f, 0.2f, 0.9f, 0.7f)
        val original = crop.toPixels(IntSize(4032, 3024))
        assertEquals(IntRect(403, 605, 3629, 2117), original)
        val preview = crop.toPixels(IntSize(2048, 1536))
        val scale = 4032f / 2048
        listOf(
            original.left to preview.left,
            original.top to preview.top,
            original.right to preview.right,
            original.bottom to preview.bottom,
        ).forEach { (o, p) -> assertTrue(abs(o - p * scale) <= 2, "edge $o vs ${p * scale}") }
    }

    @Test
    fun cornersWinOverMovingAndOutsideGrabsNothing() {
        val crop = Rect(0.25f, 0.25f, 0.75f, 0.75f)
        val size = Size(400f, 400f)
        assertEquals(CropHandle.TopLeft, crop.handleAt(Offset(110f, 95f), size, 24f))
        assertEquals(CropHandle.BottomRight, crop.handleAt(Offset(300f, 300f), size, 24f))
        assertEquals(CropHandle.Move, crop.handleAt(Offset(200f, 200f), size, 24f))
        assertNull(crop.handleAt(Offset(20f, 20f), size, 24f))
    }

    @Test
    fun backWithoutChangesLeavesAtOnce() =
        runTest {
            val viewModel = BasicEditViewModel("1", Unreadable, NoPermission)
            viewModel.onBack()

            assertEquals(BasicEditEffect.Exit, viewModel.effects.first()) // D1
            assertFalse(viewModel.state.value.exitDialog)
        }

    @Test
    fun doneDoesNothingUntilThePhotoIsLoaded() {
        val viewModel = BasicEditViewModel("1", Unreadable, NoPermission)
        viewModel.onDoneClick() // the photo could not be read: no second original() and no failure Snackbar

        assertFalse(viewModel.state.value.saving)
        assertNull(viewModel.state.value.saveError)
    }

    private fun assertClose(
        expected: Rect,
        actual: Rect,
    ) = listOf(expected.left to actual.left, expected.top to actual.top, expected.right to actual.right, expected.bottom to actual.bottom)
        .forEach { (e, a) -> assertEquals(e, a, 0.0001f, "$expected vs $actual") }

    private object Unreadable : PhotoLibrary {
        override suspend fun photos(
            offset: Int,
            limit: Int,
        ) = PhotoPage(emptyList(), null, 0)

        override suspend fun isReadable(id: String) = false

        override suspend fun thumbnail(
            id: String,
            sizePx: Int,
        ) = null

        override suspend fun preview(
            id: String,
            maxSidePx: Int,
        ) = null

        override suspend fun original(id: String): ImageBitmap = error("unreadable")

        override suspend fun save(image: ImageBitmap): String = error("not used")
    }

    private object NoPermission : PhotoPermission {
        override fun status() = error("not used")

        override suspend fun request() = error("not used")

        override fun openSettings() = Unit
    }
}
