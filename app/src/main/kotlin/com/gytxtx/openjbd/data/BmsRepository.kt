package com.gytxtx.openjbd.data

import com.gytxtx.openjbd.protocol.JbdBasicInfo
import com.gytxtx.openjbd.protocol.JbdCellVoltages
import com.gytxtx.openjbd.protocol.JbdDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BmsRepository @Inject constructor() {
    private val _uiState = MutableStateFlow(BmsUiState.disconnected(null, null, ""))
    val uiState: StateFlow<BmsUiState> = _uiState.asStateFlow()

    fun update(value: BmsUiState) {
        _uiState.value = value
    }

    fun getSnapshot(): BmsUiState = _uiState.value
}

data class BmsUiState(
    val connected: Boolean,
    val connectionState: ConnectionState,
    val deviceName: String?,
    val deviceAddress: String?,
    val status: String?,
    val basicInfo: JbdBasicInfo?,
    val cellVoltages: JbdCellVoltages?,
    val deviceInfo: JbdDeviceInfo?,
    // Emission version: MutableStateFlow suppresses values equal to the current one, and this field
    // is bumped by every with* mutation so that re-emissions always propagate (refreshLocalizedStatus
    // depends on it to re-emit even when nothing else changed). Never remove it without replacing
    // that mechanism.
    val updatedAtMillis: Long
) {
    fun withConnectionState(
        connectionState: ConnectionState,
        connected: Boolean,
        status: String?
    ): BmsUiState = copy(
        connected = connected,
        connectionState = connectionState,
        status = status,
        updatedAtMillis = System.currentTimeMillis()
    )

    fun withBasicInfo(info: JbdBasicInfo?, status: String?): BmsUiState = copy(
        connectionState = ConnectionState.READING,
        basicInfo = info,
        status = status,
        updatedAtMillis = System.currentTimeMillis()
    )

    fun withCellVoltages(voltages: JbdCellVoltages?, status: String?): BmsUiState = copy(
        cellVoltages = voltages,
        status = status,
        updatedAtMillis = System.currentTimeMillis()
    )

    fun withDeviceInfo(info: JbdDeviceInfo?, status: String?): BmsUiState = copy(
        deviceInfo = info,
        status = status,
        updatedAtMillis = System.currentTimeMillis()
    )

    companion object {
        fun disconnected(deviceName: String?, deviceAddress: String?, status: String?): BmsUiState =
            withConnectionState(
                ConnectionState.DISCONNECTED,
                false,
                deviceName,
                deviceAddress,
                status,
                null,
                null
            )

        fun withConnectionState(
            connectionState: ConnectionState,
            connected: Boolean,
            deviceName: String?,
            deviceAddress: String?,
            status: String?,
            basicInfo: JbdBasicInfo?,
            cellVoltages: JbdCellVoltages?
        ): BmsUiState = BmsUiState(
            connected = connected,
            connectionState = connectionState,
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            status = status,
            basicInfo = basicInfo,
            cellVoltages = cellVoltages,
            deviceInfo = null,
            updatedAtMillis = System.currentTimeMillis()
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
