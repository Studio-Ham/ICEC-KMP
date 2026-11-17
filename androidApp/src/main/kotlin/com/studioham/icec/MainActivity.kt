package com.studioham.icec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studioham.icec.media.AndroidPhotoLibrary
import com.studioham.icec.media.AndroidPhotoPermission

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = createAppGraph(AndroidPhotoPermission(this), AndroidPhotoLibrary(this))
        setContent { App(graph) }
    }
}
