package com.gytxtx.openjbd

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionRecoveryTest {
    @Test
    fun rationaleAvailableRequestsPermissionAgain() {
        assertFalse(DeviceListActivity.shouldOpenAppSettings(true))
    }

    @Test
    fun rationaleUnavailableOpensAppSettings() {
        assertTrue(DeviceListActivity.shouldOpenAppSettings(false))
    }
}
