package com.gytxtx.openjbd.ble;

import java.util.UUID;

public final class BleConstants {
    public static final UUID SERVICE_UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
    public static final UUID NOTIFY_UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    public static final UUID WRITE_UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BleConstants() {
    }
}
