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
}
