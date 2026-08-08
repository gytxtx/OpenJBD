package com.gytxtx.openjbd

import android.os.Handler
import android.os.Looper

internal object BmsStateStore {
    private val listeners = mutableListOf<Listener>()
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var _snapshot: Snapshot = Snapshot.disconnected(null, null, "")

    @JvmStatic
    @Synchronized
    fun getSnapshot(): Snapshot = _snapshot

    @JvmStatic
    fun update(value: Snapshot) {
        val copy: List<Listener>
        synchronized(this) {
            _snapshot = value
            copy = listeners.toList()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifyListeners(copy, value)
        } else {
            mainHandler.post { notifyListeners(copy, value) }
        }
    }

    private fun notifyListeners(listeners: List<Listener>, value: Snapshot) {
        for (listener in listeners) {
            listener.onBmsStateChanged(value)
        }
    }

    @JvmStatic
    @Synchronized
    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    @JvmStatic
    @Synchronized
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    interface Listener {
        fun onBmsStateChanged(snapshot: Snapshot)
    }

    class Snapshot
    internal constructor(
        @JvmField val connected: Boolean,
        @JvmField val connectionState: ConnectionState,
        @JvmField val deviceName: String?,
        @JvmField val deviceAddress: String?,
        @JvmField val status: String?,
        @JvmField val basicInfo: com.gytxtx.openjbd.protocol.JbdBasicInfo?,
        @JvmField val cellVoltages: com.gytxtx.openjbd.protocol.JbdCellVoltages?,
        @JvmField val deviceInfo: com.gytxtx.openjbd.protocol.JbdDeviceInfo?,
        @JvmField val updatedAtMillis: Long
    ) {
        constructor(
            connected: Boolean,
            deviceName: String?,
            deviceAddress: String?,
            status: String?,
            basicInfo: com.gytxtx.openjbd.protocol.JbdBasicInfo?,
            cellVoltages: com.gytxtx.openjbd.protocol.JbdCellVoltages?,
            updatedAtMillis: Long
        ) : this(
            connected,
            if (connected) ConnectionState.READY else ConnectionState.DISCONNECTED,
            deviceName,
            deviceAddress,
            status,
            basicInfo,
            cellVoltages,
            null,
            updatedAtMillis
        )

        fun withStatus(connected: Boolean, status: String?): Snapshot =
            withConnectionState(
                if (connected) ConnectionState.READY else ConnectionState.DISCONNECTED,
                connected,
                status
            )

        fun withConnectionState(
            connectionState: ConnectionState,
            connected: Boolean,
            status: String?
        ): Snapshot = Snapshot(
            connected,
            connectionState,
            deviceName,
            deviceAddress,
            status,
            basicInfo,
            cellVoltages,
            deviceInfo,
            System.currentTimeMillis()
        )

        fun withBasicInfo(
            info: com.gytxtx.openjbd.protocol.JbdBasicInfo?,
            status: String?
        ): Snapshot = Snapshot(
            connected,
            ConnectionState.READING,
            deviceName,
            deviceAddress,
            status,
            info,
            cellVoltages,
            deviceInfo,
            System.currentTimeMillis()
        )

        fun withCellVoltages(
            voltages: com.gytxtx.openjbd.protocol.JbdCellVoltages?,
            status: String?
        ): Snapshot = Snapshot(
            connected,
            connectionState,
            deviceName,
            deviceAddress,
            status,
            basicInfo,
            voltages,
            deviceInfo,
            System.currentTimeMillis()
        )

        fun withDeviceInfo(
            info: com.gytxtx.openjbd.protocol.JbdDeviceInfo?,
            status: String?
        ): Snapshot = Snapshot(
            connected,
            connectionState,
            deviceName,
            deviceAddress,
            status,
            basicInfo,
            cellVoltages,
            info,
            System.currentTimeMillis()
        )

        companion object {
            @JvmStatic
            fun disconnected(
                deviceName: String?,
                deviceAddress: String?,
                status: String?
            ): Snapshot = withConnectionState(
                ConnectionState.DISCONNECTED,
                false,
                deviceName,
                deviceAddress,
                status,
                null,
                null
            )

            @JvmStatic
            fun withConnectionState(
                connectionState: ConnectionState,
                connected: Boolean,
                deviceName: String?,
                deviceAddress: String?,
                status: String?,
                basicInfo: com.gytxtx.openjbd.protocol.JbdBasicInfo?,
                cellVoltages: com.gytxtx.openjbd.protocol.JbdCellVoltages?
            ): Snapshot = Snapshot(
                connected,
                connectionState,
                deviceName,
                deviceAddress,
                status,
                basicInfo,
                cellVoltages,
                null,
                System.currentTimeMillis()
            )
        }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        DISCOVERING_SERVICES,
        ENABLING_NOTIFICATIONS,
        READING,
        WAITING_RECONNECT,
        BLUETOOTH_UNAVAILABLE,
        BLUETOOTH_OFF,
        PERMISSION_REQUIRED,
        INVALID_DEVICE,
        CONNECTION_FAILED,
        SERVICE_DISCOVERY_FAILED,
        SERVICE_NOT_FOUND,
        CHARACTERISTICS_NOT_FOUND,
        NOTIFICATIONS_FAILED,
        ERROR,
        PARSE_ERROR,
        READY
    }
}
