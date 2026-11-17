package com.studioham.icec.feature.editor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.media.Photo
import com.studioham.icec.media.PhotoLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditorTest {
    private object NoLibrary : PhotoLibrary {
        override suspend fun photos() = emptyList<Photo>()

        override suspend fun isReadable(id: String) = true

        override suspend fun thumbnail(
            id: String,
            sizePx: Int,
        ) = null
    }

    private val faces = List(3) { FaceBounds(0.1f * it, 0.1f, 0.1f * it + 0.05f, 0.2f) }

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selectionDrivesSelectAllAndSave() {
        val viewModel = EditorViewModel("1", faces, NoLibrary)
        assertFalse(viewModel.state.value.canSave) // Detected ≠ Selected

        viewModel.onFaceClick(1)
        assertEquals(setOf(1), viewModel.state.value.selected)
        assertTrue(viewModel.state.value.canSave)

        viewModel.onSelectAllClick() // something selected → clear all
        assertEquals(emptySet(), viewModel.state.value.selected)
        assertFalse(viewModel.state.value.canSave)

        viewModel.onSelectAllClick() // nothing selected → select all
        assertEquals(setOf(0, 1, 2), viewModel.state.value.selected)

        viewModel.onFaceClick(2)
        assertEquals(setOf(0, 1), viewModel.state.value.selected)
    }

    @Test
    fun backAsksFirstAndKeepEditingKeepsSelection() {
        val viewModel = EditorViewModel("1", faces, NoLibrary)
        viewModel.onFaceClick(0)

        viewModel.onBack()
        assertTrue(viewModel.state.value.exitDialog)

        viewModel.onKeepEditing()
        assertFalse(viewModel.state.value.exitDialog)
        assertEquals(setOf(0), viewModel.state.value.selected)
    }

    @Test
    fun mosaicBlocksAreAnEighthOfTheFaceWidth() {
        assertEquals(IntSize(8, 10), mosaicGrid(IntRect(0, 0, 80, 100)))
        assertEquals(IntSize(8, 1), mosaicGrid(IntRect(0, 0, 80, 2)))
        assertEquals(IntRect(100, 50, 300, 150), FaceBounds(0.25f, 0.25f, 0.75f, 0.75f).toPixels(400, 200))
    }

    @Test
    fun facesMapIntoTheFittedPhoto() {
        val fit = fitRect(IntSize(400, 200), Size(200f, 200f)) // letterboxed: 200 x 100 at y = 50
        assertEquals(Rect(0f, 50f, 200f, 150f), fit)
        assertEquals(Rect(50f, 75f, 150f, 125f), fit.map(IntRect(100, 50, 300, 150), 400))
    }

    @Test
    fun faceCropStaysInsideThePhoto() {
        val crop = faceCrop(IntRect(0, 0, 50, 50), 400, 200)
        assertEquals(IntRect(0, 0, 70, 70), crop)
    }
}
