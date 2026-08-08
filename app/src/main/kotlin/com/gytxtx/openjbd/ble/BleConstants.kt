package com.gytxtx.openjbd.ble

import java.util.UUID

object BleConstants {
    @JvmField
    val SERVICE_UUID: UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")

    @JvmField
    val NOTIFY_UUID: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")

    @JvmField
    val WRITE_UUID: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")

    @JvmField
    val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
