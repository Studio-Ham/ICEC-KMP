package com.studioham.icec.feature.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.face.FaceDetector
import com.studioham.icec.media.PhotoLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface FaceAnalysisState {
    data object Analyzing : FaceAnalysisState

    data class Detected(
        val faces: List<FaceBounds>,
    ) : FaceAnalysisState

    data object NoFace : FaceAnalysisState

    data object Failed : FaceAnalysisState

    data object Timeout : FaceAnalysisState
}

/**
 * UX-002, UX-037, UX-038: analyzes one photo per entry and keeps the result until the screen leaves (UX-040).
 * [timeout] is 10 seconds by user decision on 2026-11-17.
 */
class FaceAnalysisViewModel(
    val photoId: String,
    private val detector: FaceDetector,
    private val library: PhotoLibrary,
    private val timeout: Duration = 10.seconds,
) : ViewModel() {
    private val _state = MutableStateFlow<FaceAnalysisState>(FaceAnalysisState.Analyzing)
    val state: StateFlow<FaceAnalysisState> = _state.asStateFlow()

    init {
        analyze()
    }

    /** UX-038: analyze the same photo again from AnalysisFailed or AnalysisTimeout. */
    fun onRetryClick() {
        if (_state.value == FaceAnalysisState.Analyzing) return
        _state.value = FaceAnalysisState.Analyzing
        analyze()
    }

    private fun analyze() {
        viewModelScope.launch {
            _state.value =
                try {
                    withTimeout(timeout) { detector.detect(photoId) }
                        .let { if (it.isEmpty()) FaceAnalysisState.NoFace else FaceAnalysisState.Detected(it) }
                } catch (_: TimeoutCancellationException) {
                    FaceAnalysisState.Timeout
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    FaceAnalysisState.Failed
                }
        }
    }

    suspend fun background(sizePx: Int): ImageBitmap? = library.thumbnail(photoId, sizePx)
}
