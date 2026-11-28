package com.studioham.icec.feature.editor

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntRect
import com.studioham.icec.face.FaceBounds
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
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class EditorSaveTest {
    /** A 16 x 8 image where every pixel has its own color. */
    private fun gradient(): ImageBitmap {
        val image = ImageBitmap(16, 8)
        val canvas = Canvas(image)
        for (x in 0 until 16) {
            for (y in 0 until 8) {
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, Paint().apply { color = Color(x * 16, y * 32, 128) })
            }
        }
        return image
    }

    private class FakeLibrary(
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

        override suspend fun original(id: String) = ImageBitmap(32, 32)

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
    fun mosaicFillsEachBlockWithOneColorAndLeavesTheRestAlone() {
        val image = gradient()
        val result = image.withMosaic(listOf(IntRect(0, 0, 16, 8))).toPixelMap()
        // 16 px wide face → 8 columns of 2 px blocks, 4 rows.
        for (bx in 0 until 8) {
            for (by in 0 until 4) {
                val first = result[bx * 2, by * 2]
                assertEquals(first, result[bx * 2 + 1, by * 2 + 1], "block $bx,$by")
            }
        }
        val half = image.withMosaic(listOf(IntRect(0, 0, 8, 4))).toPixelMap()
        val original = image.toPixelMap()
        assertEquals(original[12, 6], half[12, 6]) // outside the face: unchanged
        assertNotEquals(original[0, 0], original[1, 0])
    }

    @Test
    fun saveRendersTheSelectedFacesAndReportsTheNewPhoto() =
        runTest {
            val library = FakeLibrary()
            val viewModel = EditorViewModel("1", listOf(FaceBounds(0.25f, 0.25f, 0.75f, 0.75f)), library, NoPermission)
            viewModel.onFaceClick(0)

            viewModel.onSaveClick()

            assertEquals(EditorEffect.Saved("new"), viewModel.effects.first())
            assertEquals(32, library.saved?.width) // original resolution
            assertEquals(false, viewModel.state.first { !it.saving }.saving)
        }

    @Test
    fun saveFailureKeepsTheSelectionAndMapsTheError() =
        runTest {
            val library = FakeLibrary(failure = SaveException.NoSpace())
            val viewModel = EditorViewModel("1", listOf(FaceBounds(0.1f, 0.1f, 0.2f, 0.2f)), library, NoPermission)
            viewModel.onFaceClick(0)

            viewModel.onSaveClick()
            val failed = viewModel.state.first { it.saveError != null }

            assertEquals(SaveError.NoSpace, failed.saveError)
            assertEquals(setOf(0), failed.selected) // UX-016

            library.failure = IllegalStateException("encoder")
            viewModel.onSaveClick()
            assertEquals(SaveError.Failed, viewModel.state.first { it.saveError == SaveError.Failed }.saveError)

            library.failure = null
            viewModel.onSaveErrorAction() // Failed → try again
            assertEquals(EditorEffect.Saved("new"), viewModel.effects.first())
        }
}
