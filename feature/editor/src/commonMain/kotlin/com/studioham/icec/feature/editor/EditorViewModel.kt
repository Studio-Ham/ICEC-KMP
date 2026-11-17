package com.studioham.icec.feature.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.media.PhotoLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorState(
    val image: ImageBitmap? = null,
    val faces: List<FaceBounds>,
    val selected: Set<Int> = emptySet(),
    val exitDialog: Boolean = false,
) {
    /** UX-039: save needs at least one selected face. */
    val canSave get() = selected.isNotEmpty()
}

/** Editor Ready state: every face starts unselected (`Detected ≠ Selected`). */
class EditorViewModel(
    photoId: String,
    faces: List<FaceBounds>,
    library: PhotoLibrary,
) : ViewModel() {
    // Left to right, so the face list follows the photo.
    private val _state = MutableStateFlow(EditorState(faces = faces.sortedBy { it.left }))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        // ponytail: one 2048px image for display and preview; #26 renders the saved photo at full resolution.
        viewModelScope.launch {
            val image = library.thumbnail(photoId, IMAGE_PX)
            _state.update { it.copy(image = image) }
        }
    }

    fun onFaceClick(index: Int) = _state.update { it.copy(selected = it.selected.toggle(index)) }

    /** Select all when nothing is selected, otherwise clear all (UX-039). */
    fun onSelectAllClick() = _state.update { it.copy(selected = if (it.selected.isEmpty()) it.faces.indices.toSet() else emptySet()) }

    /** Every back action asks first, whatever the selection. */
    fun onBack() = _state.update { it.copy(exitDialog = true) }

    fun onKeepEditing() = _state.update { it.copy(exitDialog = false) }

    private fun Set<Int>.toggle(index: Int) = if (index in this) this - index else this + index

    private companion object {
        const val IMAGE_PX = 2048
    }
}
