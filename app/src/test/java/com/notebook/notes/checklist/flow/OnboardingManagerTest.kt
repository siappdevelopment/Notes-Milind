package com.notebook.notes.checklist.flow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingManagerTest {

    private class FakeOnboardingStore(
        override var hasSeenOnboarding: Boolean = false,
        override var launchesSinceOnboarding: Int = 0,
    ) : OnboardingStore

    private val store = FakeOnboardingStore()
    private var repeatCount = 0
    private val manager = OnboardingManager(store) { repeatCount }

    /** One cold launch the way SplashActivity does it; returns where the app went. */
    private fun coldLaunch(finishOnboarding: Boolean = true): String =
        if (manager.shouldShowOnboarding()) {
            if (finishOnboarding) manager.onOnboardingCompleted()
            ONBOARDING
        } else {
            manager.recordNormalLaunch()
            MAIN
        }

    private fun launches(count: Int): List<String> = List(count) { coldLaunch() }

    // --- first launch ---

    @Test
    fun `fresh install shows onboarding whatever the repeat count`() {
        for (count in listOf(-5, 0, 1, 2, 10)) {
            val fresh = OnboardingManager(FakeOnboardingStore()) { count }
            assertTrue("count=$count", fresh.shouldShowOnboarding())
        }
    }

    // --- repeat count 0: once only ---

    @Test
    fun `count 0 shows onboarding only on the first launch`() {
        repeatCount = 0
        assertEquals(listOf(ONBOARDING, MAIN, MAIN, MAIN, MAIN), launches(5))
    }

    // --- repeat count 1 ---

    @Test
    fun `count 1 alternates onboarding and main`() {
        repeatCount = 1
        assertEquals(listOf(ONBOARDING, MAIN, ONBOARDING, MAIN, ONBOARDING), launches(5))
    }

    // --- repeat count 2 ---

    @Test
    fun `count 2 shows onboarding every third launch forever`() {
        repeatCount = 2
        assertEquals(
            listOf(ONBOARDING, MAIN, MAIN, ONBOARDING, MAIN, MAIN, ONBOARDING),
            launches(7),
        )
    }

    // --- state transitions ---

    @Test
    fun `completing onboarding marks it seen and resets the counter`() {
        store.launchesSinceOnboarding = 7

        manager.onOnboardingCompleted()

        assertTrue(store.hasSeenOnboarding)
        assertEquals(0, store.launchesSinceOnboarding)
    }

    @Test
    fun `recording a normal launch increments the counter by one`() {
        store.hasSeenOnboarding = true

        manager.recordNormalLaunch()
        manager.recordNormalLaunch()

        assertEquals(2, store.launchesSinceOnboarding)
    }

    @Test
    fun `asking shouldShowOnboarding repeatedly does not change state`() {
        repeatCount = 2
        launches(2) // onboarding, main

        val before = store.launchesSinceOnboarding
        repeat(5) { manager.shouldShowOnboarding() }

        assertEquals(before, store.launchesSinceOnboarding)
    }

    // --- edge cases ---

    @Test
    fun `negative repeat count behaves like 0`() {
        repeatCount = -3
        assertEquals(listOf(ONBOARDING, MAIN, MAIN, MAIN), launches(4))
    }

    @Test
    fun `killing the app during first onboarding shows it again next launch`() {
        repeatCount = 2
        assertEquals(ONBOARDING, coldLaunch(finishOnboarding = false))
        assertEquals(ONBOARDING, coldLaunch(finishOnboarding = false))
        assertEquals(ONBOARDING, coldLaunch(finishOnboarding = true))
        assertEquals(MAIN, coldLaunch())
    }

    @Test
    fun `killing the app during a repeated onboarding shows it again next launch`() {
        repeatCount = 1
        launches(2) // onboarding, main - the next launch is a repeat
        assertEquals(ONBOARDING, coldLaunch(finishOnboarding = false))
        assertEquals(ONBOARDING, coldLaunch(finishOnboarding = true))
        assertEquals(MAIN, coldLaunch())
    }

    @Test
    fun `raising the repeat count later keeps counting from the saved counter`() {
        repeatCount = 1
        launches(2) // onboarding, main -> counter = 1, onboarding due
        assertTrue(manager.shouldShowOnboarding())
        manager.onOnboardingCompleted()

        repeatCount = 3
        assertEquals(listOf(MAIN, MAIN, MAIN, ONBOARDING), launches(4))
    }

    @Test
    fun `lowering the repeat count below the saved counter shows onboarding`() {
        repeatCount = 5
        launches(4) // onboarding + 3 mains -> counter = 3
        assertFalse(manager.shouldShowOnboarding())

        repeatCount = 2
        assertTrue(manager.shouldShowOnboarding())
    }

    @Test
    fun `switching to 0 later stops onboarding after the saved counter is reached`() {
        repeatCount = 1
        launches(2) // onboarding due now
        repeatCount = 0
        assertFalse(manager.shouldShowOnboarding())
        assertEquals(listOf(MAIN, MAIN), launches(2))
    }

    @Test
    fun `user who already completed onboarding before this feature is not shown it again`() {
        val legacy = FakeOnboardingStore(hasSeenOnboarding = true, launchesSinceOnboarding = 0)
        val legacyManager = OnboardingManager(legacy) { 0 }

        assertFalse(legacyManager.shouldShowOnboarding())
    }

    private companion object {
        const val ONBOARDING = "Onboarding"
        const val MAIN = "Main"
    }
}
