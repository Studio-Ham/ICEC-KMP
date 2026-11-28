package com.studioham.icec

import kotlin.test.Test
import kotlin.test.assertEquals

class BackStackTest {
    private val gallery = Route.Gallery()
    private val editor = Route.Editor("photo", emptyList())

    @Test
    fun repeatedPopRemovesOnlyTheSourceRoute() {
        val backStack = mutableListOf<Route>(Route.Home, gallery, editor)

        backStack.pop(editor)
        backStack.pop(editor)

        assertEquals(listOf<Route>(Route.Home, gallery), backStack)
    }

    @Test
    fun popKeepsHomeAtTheRoot() {
        val backStack = mutableListOf<Route>(Route.Home)

        backStack.pop(Route.Home)

        assertEquals(listOf<Route>(Route.Home), backStack)
    }

    @Test
    fun repeatedPushAddsOnlyOneDestination() {
        val backStack = mutableListOf<Route>(Route.Home)

        backStack.push(Route.Home, gallery)
        backStack.push(Route.Home, gallery)

        assertEquals(listOf<Route>(Route.Home, gallery), backStack)
    }

    @Test
    fun callbacksFromAnInactiveRouteDoNotChangeTheStack() {
        val backStack = mutableListOf<Route>(Route.Home, gallery, editor)

        backStack.pop(gallery)
        backStack.push(gallery, Route.FaceAnalysis("another-photo"))

        assertEquals(listOf<Route>(Route.Home, gallery, editor), backStack)
    }

    @Test
    fun mosaicReplacesThePaletteSoLeavingReturnsToTheGallery() {
        val palette = Route.PhotoPreview("photo")
        val analysis = Route.FaceAnalysis("photo")
        val backStack = mutableListOf(Route.Home, gallery, palette)

        backStack.replace(palette, analysis)
        backStack.replace(palette, Route.FaceAnalysis("other")) // a second tap from the gone palette
        assertEquals(listOf(Route.Home, gallery, analysis), backStack)

        backStack.pop(analysis) // back, no face, failed, timeout or `다른 사진 선택`
        assertEquals(listOf<Route>(Route.Home, gallery), backStack)
    }

    @Test
    fun basicEditStacksOnThePalette() {
        val palette = Route.PhotoPreview("photo")
        val edit = Route.BasicEdit("photo")
        val backStack = mutableListOf(Route.Home, gallery, palette)

        backStack.push(palette, edit)
        backStack.push(edit, Route.Result("saved"))
        backStack.pop(Route.Result("saved")) // `다시 편집하기`
        backStack.pop(edit)

        assertEquals(listOf(Route.Home, gallery, palette), backStack)
    }
}
