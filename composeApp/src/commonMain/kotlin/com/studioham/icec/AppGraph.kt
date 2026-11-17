package com.studioham.icec

import com.studioham.icec.feature.gallery.GalleryViewModel
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPermission
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph
interface AppGraph {
    val galleryViewModel: GalleryViewModel

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides photoPermission: PhotoPermission,
            @Provides photoLibrary: PhotoLibrary,
        ): AppGraph
    }
}

fun createAppGraph(
    photoPermission: PhotoPermission,
    photoLibrary: PhotoLibrary,
): AppGraph = createGraphFactory<AppGraph.Factory>().create(photoPermission, photoLibrary)
