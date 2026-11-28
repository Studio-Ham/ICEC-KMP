package com.studioham.icec.feature.editor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPage
import com.studioham.icec.media.PhotoPermission
import com.studioham.icec.media.SaveException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BasicEditSaveTest {
    /** A [width] x [height] image where every pixel has its own color. */
    private fun gradient(
        width: Int = 3,
        height: Int = 2,
    ): ImageBitmap {
        val image = ImageBitmap(width, height)
        val canvas = Canvas(image)
        for (x in 0 until width) {
            for (y in 0 until height) {
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, Paint().apply { color = Color(x * 60, y * 90, 128) })
            }
        }
        return image
    }

    private class FakeLibrary(
        private val original: ImageBitmap,
        var failure: Exception? = null,
    ) : PhotoLibrary {
        var saved: ImageBitmap? = null

        override suspend fun photos(
            offset: Int,
            limit: Int,
        ) = PhotoPage(emptyList(), null, 0)

        override suspend fun isReadable(id: String) = true

        override suspend fun thumbnail(
            id: String,
            sizePx: Int,
        ) = null

        override suspend fun preview(
            id: String,
            maxSidePx: Int,
        ) = null

        override suspend fun original(id: String) = original

        override suspend fun save(image: ImageBitmap): String {
            failure?.let { throw it }
            saved = image
            return "new"
        }
    }

    private object NoPermission : PhotoPermission {
        override fun status() = error("not used")

        override suspend fun request() = error("not used")

        override fun openSettings() = Unit
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun everyTurnAndFlipMovesEachPixelExactly() {
        val source = gradient()
        val pixels = source.toPixelMap()
        val w = source.width
        val h = source.height
        for (turns in 0..3) {
            for (flipped in listOf(false, true)) {
                val result = source.edited(BasicEdit(turns = turns, flipped = flipped)).toPixelMap()
                val turned = turnedSize(w, h, turns)
                assertEquals(turned.width, result.width)
                for (x in 0 until w) {
                    for (y in 0 until h) {
                        // Counter-clockwise quarter turns (D3), then the left-right mirror.
                        val (tx, ty) =
                            when (turns) {
                                1 -> y to w - 1 - x
                                2 -> w - 1 - x to h - 1 - y
                                3 -> h - 1 - y to x
                                else -> x to y
                            }
                        val dx = if (flipped) turned.width - 1 - tx else tx
                        assertEquals(pixels[x, y], result[dx, ty], "turns=$turns flipped=$flipped at $x,$y")
                    }
                }
            }
        }
    }

    @Test
    fun cropKeepsOnlyTheBoxAtFullResolution() {
        val source = gradient(4, 2)
        val result = source.edited(BasicEdit(crop = Rect(0.5f, 0f, 1f, 1f))).toPixelMap()
        assertEquals(2, result.width)
        assertEquals(source.toPixelMap()[2, 1], result[0, 1])
    }

    @Test
    fun unchangedSaveWorksAndARetryThatFailsAtOnceIsANewFailure() =
        runTest {
            val library = FakeLibrary(gradient(16, 8), failure = IllegalStateException("encoder"))
            val viewModel = BasicEditViewModel("1", library, NoPermission)
            viewModel.state.first { it.image != null }

            viewModel.onDoneClick() // D2: no change needed once loaded
            assertEquals(SaveError.Failed, viewModel.state.first { it.saveError != null }.saveError)
            viewModel.onSaveErrorAction() // `다시 시도` fails again
            // The same error again is a new failure, so the Snackbar gets its own 4 seconds.
            assertEquals(SaveError.Failed, viewModel.state.first { it.saveFailures == 2 }.saveError)

            library.failure = null
            viewModel.onSaveErrorAction()
            assertEquals(BasicEditEffect.Saved("new"), viewModel.effects.first())
            assertEquals(16, library.saved?.width) // unchanged original size
        }

    @Test
    fun theExitDialogBlocksSavingAndKeepsTheSaveError() =
        runTest {
            val library = FakeLibrary(gradient(16, 8), failure = IllegalStateException("encoder"))
            val viewModel = BasicEditViewModel("1", library, NoPermission)
            viewModel.state.first { it.image != null }
            viewModel.onRotateClick()
            viewModel.onDoneClick()
            viewModel.state.first { it.saveError != null }

            viewModel.onBack() // changed, so D1 asks first
            library.failure = null
            viewModel.onSaveErrorAction() // the hidden Snackbar's `다시 시도`
            viewModel.onDoneClick()
            val state = viewModel.state.value
            assertEquals(true, state.exitDialog)
            assertEquals(false, state.saving) // never both at once
            assertEquals(SaveError.Failed, state.saveError)

            viewModel.onKeepEditing()
            viewModel.onSaveErrorAction()
            assertEquals(BasicEditEffect.Saved("new"), viewModel.effects.first())
        }

    @Test
    fun saveRendersTheEditOnTheOriginalAndFailureKeepsIt() =
        runTest {
            val library = FakeLibrary(gradient(16, 8), failure = SaveException.NoSpace())
            val viewModel = BasicEditViewModel("1", library, NoPermission)
            viewModel.state.first { it.image != null }
            viewModel.onRotateClick()

            viewModel.onDoneClick()
            val failed = viewModel.state.first { it.saveError != null }
            assertEquals(SaveError.NoSpace, failed.saveError)
            assertEquals(1, failed.edit.turns) // UX-016: the edit stays

            library.failure = null
            viewModel.onDoneClick()
            assertEquals(BasicEditEffect.Saved("new"), viewModel.effects.first())
            assertEquals(8, library.saved?.width) // turned original resolution
            assertEquals(16, library.saved?.height)
        }
}
