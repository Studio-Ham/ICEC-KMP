package com.studioham.icec.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studioham.icec.media.PhotoAccess
import com.studioham.icec.media.PhotoPermission
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class GalleryViewModel(
    private val permission: PhotoPermission,
) : ViewModel() {
    private val _access = MutableStateFlow(permission.status())
    val access: StateFlow<PhotoAccess> = _access.asStateFlow()

    /** Re-reads the permission, e.g. after returning from the OS settings. */
    fun refresh() {
        _access.value = permission.status()
    }

    /** UX-020~022: ask while the system can ask, otherwise send the user to the OS settings. */
    fun onAllowClick() {
        if (_access.value == PhotoAccess.Blocked) {
            permission.openSettings()
        } else {
            viewModelScope.launch { _access.value = permission.request() }
        }
    }
}
