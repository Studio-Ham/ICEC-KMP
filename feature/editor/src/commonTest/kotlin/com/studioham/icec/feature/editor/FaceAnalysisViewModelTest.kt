package com.studioham.icec.feature.editor

import com.studioham.icec.face.FaceBounds
import com.studioham.icec.face.FaceDetector
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

@OptIn(ExperimentalCoroutinesApi::class)
class FaceAnalysisViewModelTest {
    private object NoLibrary : PhotoLibrary {
        override suspend fun photos() = emptyList<Photo>()

        override suspend fun isReadable(id: String) = true

        override suspend fun thumbnail(
            id: String,
            sizePx: Int,
        ) = null
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun analyze(detector: FaceDetector) = FaceAnalysisViewModel("1", detector, NoLibrary).state.value

    @Test
    fun resultMapsToState() {
        val face = FaceBounds(0.1f, 0.1f, 0.2f, 0.2f)

        assertEquals(FaceAnalysisState.Detected(listOf(face)), analyze { listOf(face) })
        assertEquals(FaceAnalysisState.NoFace, analyze { emptyList() })
        assertEquals(FaceAnalysisState.Failed, analyze { error("ML Kit down") })
    }
}
