package com.studioham.icec.feature.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPermission
import com.studioham.icec.media.SaveException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Figma Editor Snackbar cases 2–4 (316:4050, 316:3747, 316:3897). */
enum class SaveError { NoSpace, NoPermission, Failed }

/** Runs [save] and maps a failure to its Snackbar case (UX-016); null when it worked. Mosaic and basic edit share it. */
internal suspend fun saveOrError(save: suspend () -> Unit): SaveError? =
    try {
        save()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (_: SaveException.NoSpace) {
        SaveError.NoSpace
    } catch (_: SaveException.NoPermission) {
        SaveError.NoPermission
    } catch (_: Exception) {
        SaveError.Failed
    }

data class EditorState(
    val image: ImageBitmap? = null,
    val faces: List<FaceBounds>,
    val selected: Set<Int> = emptySet(),
    val exitDialog: Boolean = false,
    val saving: Boolean = false,
    val saveError: SaveError? = null,
    /** Counts failed saves, so the screen restarts the Snackbar timer even when the same error comes back at once. */
    val saveFailures: Int = 0,
) {
    /** UX-039: save needs at least one selected face. */
    val canSave get() = selected.isNotEmpty()
}

sealed interface EditorEffect {
    /** Saved as a new photo with [photoId]; the original is unchanged. */
    data class Saved(
        val photoId: String,
    ) : EditorEffect
}

/** Editor Ready and Saving: every face starts unselected (`Detected ≠ Selected`). */
class EditorViewModel(
    private val photoId: String,
    faces: List<FaceBounds>,
    private val library: PhotoLibrary,
    private val permission: PhotoPermission,
) : ViewModel() {
    // Left to right, so the face list follows the photo.
    private val _state = MutableStateFlow(EditorState(faces = faces.sortedBy { it.left }))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _effects = Channel<EditorEffect>(Channel.BUFFERED)
    val effects: Flow<EditorEffect> = _effects.receiveAsFlow()

    init {
        // The screen shows a 2048px image; saving renders the original at full resolution.
        viewModelScope.launch {
            val image = library.preview(photoId, IMAGE_PX)
            _state.update { it.copy(image = image) }
        }
    }

    fun onFaceClick(index: Int) = edit { it.copy(selected = it.selected.toggle(index)) }

    /** Select all when nothing is selected, otherwise clear all (UX-039). */
    fun onSelectAllClick() = edit { it.copy(selected = if (it.selected.isEmpty()) it.faces.indices.toSet() else emptySet()) }

    /** Every back action asks first, whatever the selection. Saving blocks back (UX-014). */
    fun onBack() = edit { it.copy(exitDialog = true) }

    fun onKeepEditing() = _state.update { it.copy(exitDialog = false) }

    /** UX-014: renders the selected faces on the original and saves a new photo; one save at a time. */
    fun onSaveClick() {
        val current = _state.value
        // Saving and the exit dialog never show together.
        if (!current.canSave || current.saving || current.exitDialog) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val error =
                saveOrError {
                    val original = library.original(photoId)
                    val faces = current.selected.map { current.faces[it].toPixels(original.width, original.height) }
                    val result = withContext(Dispatchers.Default) { original.withMosaic(faces) }
                    _effects.send(EditorEffect.Saved(library.save(result)))
                }
            // UX-016: a failure keeps the selection and shows a Snackbar in the Editor.
            _state.update { it.copy(saving = false, saveError = error, saveFailures = it.saveFailures + if (error != null) 1 else 0) }
        }
    }

    /** The Snackbar action: open settings for a missing permission, otherwise try again. */
    fun onSaveErrorAction() {
        // The Snackbar is hidden behind the exit dialog; keep its error for when the dialog closes.
        if (_state.value.exitDialog) return
        val error = _state.value.saveError
        // Dismiss first, so a retry that fails at once keeps its new error.
        onSaveErrorDismiss()
        when (error) {
            SaveError.NoPermission -> permission.openSettings()
            SaveError.Failed -> onSaveClick()
            SaveError.NoSpace, null -> Unit
        }
    }

    fun onSaveErrorDismiss() = _state.update { it.copy(saveError = null) }

    private fun edit(change: (EditorState) -> EditorState) = _state.update { if (it.saving) it else change(it) }

    private fun Set<Int>.toggle(index: Int) = if (index in this) this - index else this + index

    private companion object {
        const val IMAGE_PX = 2048
    }
}
