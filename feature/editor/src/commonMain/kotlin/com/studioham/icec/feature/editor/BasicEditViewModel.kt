package com.studioham.icec.feature.editor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPermission
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

data class BasicEditState(
    /** The upright original scaled down for the screen; null until loaded or when it cannot be read. */
    val image: ImageBitmap? = null,
    val edit: BasicEdit = BasicEdit(),
    val exitDialog: Boolean = false,
    val saving: Boolean = false,
    val saveError: SaveError? = null,
    /** Counts failed saves, so the screen restarts the Snackbar timer even when the same error comes back at once. */
    val saveFailures: Int = 0,
)

sealed interface BasicEditEffect {
    /** Saved as a new photo with [photoId]; the original is unchanged. */
    data class Saved(
        val photoId: String,
    ) : BasicEditEffect

    /** D1: back with no change leaves without asking. */
    data object Exit : BasicEditEffect
}

/** Wiki 기본 편집: crop, a quarter turn and a left-right flip on the original photo, saved as a new photo. */
class BasicEditViewModel(
    private val photoId: String,
    private val library: PhotoLibrary,
    private val permission: PhotoPermission,
) : ViewModel() {
    private val _state = MutableStateFlow(BasicEditState())
    val state: StateFlow<BasicEditState> = _state.asStateFlow()

    private val _effects = Channel<BasicEditEffect>(Channel.BUFFERED)
    val effects: Flow<BasicEditEffect> = _effects.receiveAsFlow()

    init {
        // The preview comes from original() so it has exactly the save's orientation and shape.
        viewModelScope.launch {
            val image =
                try {
                    val original = library.original(photoId)
                    withContext(Dispatchers.Default) { original.scaledToFit(PREVIEW_PX) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            _state.update { it.copy(image = image) }
        }
    }

    fun onRatioClick(ratio: CropRatio) = editImage { edit, image -> edit.withRatio(ratio, image.width, image.height) }

    fun onCropChange(crop: Rect) = editImage { edit, _ -> edit.copy(crop = crop) }

    fun onRotateClick() = editImage { edit, image -> edit.rotated(image.width, image.height) }

    fun onFlipClick() = editImage { edit, _ -> edit.mirrored() }

    /** D1: ask only when something changed. Saving blocks back (UX-014). */
    fun onBack() {
        val current = _state.value
        when {
            current.saving -> Unit
            current.edit.isIdentity -> _effects.trySend(BasicEditEffect.Exit)
            else -> _state.update { it.copy(exitDialog = true) }
        }
    }

    fun onKeepEditing() = _state.update { it.copy(exitDialog = false) }

    /**
     * D2: always enabled, even with no change, once the photo is loaded. Before that (or when it cannot be read) it does
     * nothing, so the original is not decoded twice at once. D4 (UX-014, UX-016): renders on the original, one save at a time.
     */
    fun onDoneClick() {
        val current = _state.value
        // Saving and the exit dialog never show together.
        if (current.saving || current.exitDialog || current.image == null) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val error =
                saveOrError {
                    val original = library.original(photoId)
                    val result = withContext(Dispatchers.Default) { original.edited(current.edit) }
                    _effects.send(BasicEditEffect.Saved(library.save(result)))
                }
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
            SaveError.Failed -> onDoneClick()
            SaveError.NoSpace, null -> Unit
        }
    }

    fun onSaveErrorDismiss() = _state.update { it.copy(saveError = null) }

    private fun editImage(change: (BasicEdit, ImageBitmap) -> BasicEdit) =
        _state.update { state ->
            val image = state.image
            if (state.saving || image == null) state else state.copy(edit = change(state.edit, image))
        }

    private companion object {
        const val PREVIEW_PX = 2048
    }
}
