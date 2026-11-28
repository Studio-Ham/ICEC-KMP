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
import com.studioham.icec.feature.result.ResultScreen
import com.studioham.icec.home.HomeScreen

sealed interface Route {
    data object Home : Route

    /** A new [session] gives a fresh Gallery at the newest photos (Result `다른 사진 편집하기`). */
    data class Gallery(
        val session: Int = 0,
    ) : Route

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
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        // Each entry owns its ViewModels, so every FaceAnalysis analyzes its own photo and is cleared on pop.
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider =
            entryProvider {
                entry<Route.Home> { route ->
                    HomeScreen(onLoadPhotoClick = { backStack.push(route, Route.Gallery()) })
                }
                entry<Route.Gallery> { route ->
                    GalleryScreen(
                        viewModel { graph.galleryViewModel },
                        onBack = { backStack.pop(route) },
                        onPhotoSelected = { backStack.push(route, Route.FaceAnalysis(it.id)) },
                    )
                }
                entry<Route.FaceAnalysis> { route ->
                    FaceAnalysisScreen(
                        viewModel { FaceAnalysisViewModel(route.photoId, graph.faceDetector, graph.photoLibrary) },
                        // Replace FaceAnalysis so leaving the Editor returns to the Gallery (UX-037).
                        onFacesDetected = { faces ->
                            if (backStack.lastOrNull() == route) {
                                backStack[backStack.lastIndex] = Route.Editor(route.photoId, faces)
                            }
                        },
                        onPickAnother = { backStack.pop(route) },
                    )
                }
                entry<Route.Editor> { route ->
                    EditorScreen(
                        viewModel { EditorViewModel(route.photoId, route.faces, graph.photoLibrary, graph.photoPermission) },
                        // Editor replaced FaceAnalysis, so leaving returns to the Gallery's previous position.
                        onExit = { backStack.pop(route) },
                        // Result sits on top of the Editor, so back returns to the same editing session.
                        onSaved = { backStack.push(route, Route.Result(it)) },
                    )
                }
                entry<Route.Result> { route ->
                    ResultScreen(
                        route.savedPhotoId,
                        graph.photoLibrary,
                        // Result sits on top of the Editor, so popping restores the editing session.
                        onEditAgain = { backStack.pop(route) },
                        onOtherPhoto = { backStack.startOver(route, gallery = true) },
                        onHome = { backStack.startOver(route, gallery = false) },
                    )
                }
            },
    )
}

internal fun MutableList<Route>.pop(from: Route) {
    if (size > 1 && last() == from) removeAt(lastIndex)
}

internal fun MutableList<Route>.push(
    from: Route,
    to: Route,
) {
    if (lastOrNull() == from) add(to)
}

/** Ends the editing session: keeps only Home, then opens a fresh Gallery when [gallery] is true (Wiki Result UX). */
private fun MutableList<Route>.startOver(
    from: Route.Result,
    gallery: Boolean,
) {
    if (lastOrNull() != from) return
    val session = filterIsInstance<Route.Gallery>().maxOfOrNull { it.session } ?: 0
    retainAll { it == Route.Home }
    if (gallery) add(Route.Gallery(session + 1))
}
