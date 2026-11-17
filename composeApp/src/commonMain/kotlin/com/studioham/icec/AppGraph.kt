package com.studioham.icec

import com.studioham.icec.face.FaceDetector
import com.studioham.icec.feature.gallery.GalleryViewModel
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPermission
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph
interface AppGraph {
    val galleryViewModel: GalleryViewModel
    val faceDetector: FaceDetector
    val photoLibrary: PhotoLibrary

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides photoPermission: PhotoPermission,
            @Provides photoLibrary: PhotoLibrary,
            @Provides faceDetector: FaceDetector,
        ): AppGraph
    }
}

fun createAppGraph(
    photoPermission: PhotoPermission,
    photoLibrary: PhotoLibrary,
    faceDetector: FaceDetector,
): AppGraph = createGraphFactory<AppGraph.Factory>().create(photoPermission, photoLibrary, faceDetector)
