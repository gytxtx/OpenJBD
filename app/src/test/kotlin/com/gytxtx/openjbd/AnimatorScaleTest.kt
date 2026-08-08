package com.gytxtx.openjbd

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatorScaleTest {
    @Test
    fun zeroAnimatorDurationScaleDisablesAnimations() {
        assertFalse(MainActivity.animationsEnabled(0f))
    }

    @Test
    fun positiveAnimatorDurationScaleEnablesAnimations() {
        assertTrue(MainActivity.animationsEnabled(0.5f))
        assertTrue(MainActivity.animationsEnabled(1f))
    }
}
