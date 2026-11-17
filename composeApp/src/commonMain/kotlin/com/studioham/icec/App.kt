package com.studioham.icec

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.studioham.icec.face.FaceBounds
import com.studioham.icec.feature.editor.EditorScreen
import com.studioham.icec.feature.editor.EditorViewModel
import com.studioham.icec.feature.editor.FaceAnalysisScreen
import com.studioham.icec.feature.editor.FaceAnalysisViewModel
import com.studioham.icec.feature.gallery.GalleryScreen
import com.studioham.icec.home.HomeScreen
import com.studioham.icec.result.ResultPlaceholder

sealed interface Route {
    data object Home : Route

    data object Gallery : Route

    data class FaceAnalysis(
        val photoId: String,
    ) : Route

    data class Editor(
        val photoId: String,
        val faces: List<FaceBounds>,
    ) : Route

    data class Result(
        val savedPhotoId: String,
    ) : Route
}

// ponytail: in-memory back stack; a cold start begins at Home (UX-034). Android keeps it across
// rotation via configChanges in the manifest.
@Composable
fun App(
    graph: AppGraph,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        // Each entry owns its ViewModels, so every FaceAnalysis analyzes its own photo and is cleared on pop.
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider =
            entryProvider {
                entry<Route.Home> { HomeScreen(onLoadPhotoClick = { backStack.add(Route.Gallery) }) }
                entry<Route.Gallery> {
                    GalleryScreen(
                        viewModel { graph.galleryViewModel },
                        onBack = { backStack.removeLastOrNull() },
                        onPhotoSelected = { backStack.add(Route.FaceAnalysis(it.id)) },
                    )
                }
                entry<Route.FaceAnalysis> { route ->
                    FaceAnalysisScreen(
                        viewModel { FaceAnalysisViewModel(route.photoId, graph.faceDetector, graph.photoLibrary) },
                        // Replace FaceAnalysis so leaving the Editor returns to the Gallery (UX-037).
                        onFacesDetected = { faces -> backStack[backStack.lastIndex] = Route.Editor(route.photoId, faces) },
                        onPickAnother = { backStack.removeLastOrNull() },
                    )
                }
                entry<Route.Editor> { route ->
                    EditorScreen(
                        viewModel { EditorViewModel(route.photoId, route.faces, graph.photoLibrary, graph.photoPermission) },
                        // Editor replaced FaceAnalysis, so leaving returns to the Gallery's previous position.
                        onExit = { backStack.removeLastOrNull() },
                        // Result sits on top of the Editor, so back returns to the same editing session.
                        onSaved = { backStack.add(Route.Result(it)) },
                    )
                }
                entry<Route.Result> {
                    ResultPlaceholder(it.savedPhotoId, graph.photoLibrary, onEditAgain = { backStack.removeLastOrNull() })
                }
            },
    )
}
