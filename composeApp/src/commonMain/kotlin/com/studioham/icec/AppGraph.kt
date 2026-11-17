package com.studioham.icec

import com.studioham.icec.feature.gallery.GalleryViewModel
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
        ): AppGraph
    }
}

fun createAppGraph(photoPermission: PhotoPermission): AppGraph = createGraphFactory<AppGraph.Factory>().create(photoPermission)
