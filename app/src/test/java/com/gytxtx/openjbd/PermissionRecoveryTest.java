package com.gytxtx.openjbd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PermissionRecoveryTest {
    @Test
    public void rationaleAvailableRequestsPermissionAgain() {
        assertFalse(DeviceListActivity.shouldOpenAppSettings(true));
    }

    @Test
    public void rationaleUnavailableOpensAppSettings() {
        assertTrue(DeviceListActivity.shouldOpenAppSettings(false));
    }
}
