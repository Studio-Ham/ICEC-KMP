package com.studioham.icec.feature.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.face.FaceDetector
import com.studioham.icec.media.PhotoLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FaceAnalysisState {
    data object Analyzing : FaceAnalysisState

    data class Detected(
        val faces: List<FaceBounds>,
    ) : FaceAnalysisState

    data object NoFace : FaceAnalysisState

    data object Failed : FaceAnalysisState
}

/** UX-002, UX-037: analyzes one photo once per entry; the result stays until the screen leaves (UX-040). */
class FaceAnalysisViewModel(
    val photoId: String,
    detector: FaceDetector,
    private val library: PhotoLibrary,
) : ViewModel() {
    private val _state = MutableStateFlow<FaceAnalysisState>(FaceAnalysisState.Analyzing)
    val state: StateFlow<FaceAnalysisState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value =
                try {
                    detector.detect(photoId).let { if (it.isEmpty()) FaceAnalysisState.NoFace else FaceAnalysisState.Detected(it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    FaceAnalysisState.Failed
                }
        }
    }

    suspend fun background(sizePx: Int): ImageBitmap? = library.thumbnail(photoId, sizePx)
}
