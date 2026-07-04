package com.gytxtx.openjbd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AnimatorScaleTest {
    @Test
    public void zeroAnimatorDurationScaleDisablesAnimations() {
        assertFalse(MainActivity.animationsEnabled(0f));
    }

    @Test
    public void positiveAnimatorDurationScaleEnablesAnimations() {
        assertTrue(MainActivity.animationsEnabled(0.5f));
        assertTrue(MainActivity.animationsEnabled(1f));
    }
}
