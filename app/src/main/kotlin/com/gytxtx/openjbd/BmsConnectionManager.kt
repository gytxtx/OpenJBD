package com.gytxtx.openjbd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.gytxtx.openjbd.ble.BleConstants
import com.gytxtx.openjbd.data.BmsRepository
import com.gytxtx.openjbd.data.BmsUiState
import com.gytxtx.openjbd.data.ConnectionState
import com.gytxtx.openjbd.protocol.JbdCommands
import com.gytxtx.openjbd.protocol.JbdDeviceInfo
import com.gytxtx.openjbd.protocol.JbdFrame
import com.gytxtx.openjbd.protocol.JbdFrameAssembler
import com.gytxtx.openjbd.protocol.JbdParseException
import com.gytxtx.openjbd.protocol.JbdParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BmsConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BmsRepository
) {
    private val handler = Handler(Looper.getMainLooper())
    private val frameAssembler = JbdFrameAssembler()
    private val commandQueue = ArrayDeque<CommandRequest>()

    private var adapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var connected = false
    private var writeInFlight = false
    private var currentCommand: CommandRequest? = null
    private var connectedDeviceName: String? = null
    private var connectedDeviceAddress: String? = null
    private var autoReconnectEnabled = false
    private var intentionalDisconnect = false
    private var reconnectScheduled = false
    private var autoReconnectAddress: String? = null
    private var autoReconnectName: String? = null
    private var reconnectAttempts = 0
    private var refreshIntervalMs = 2000L
    private var extendedInfoRequested = false

    private val pollRunnable = Runnable {
        if (!connected || writeInFlight || currentCommand != null || commandQueue.isNotEmpty()) {
            return@Runnable
        }
        enqueuePollCycle()
        drainCommandQueue()
    }

    private val commandTimeoutRunnable = Runnable {
        if (currentCommand == null) return@Runnable
        completeCurrentCommand()
    }

    private val reconnectRunnable = Runnable {
        reconnectScheduled = false
        if (!shouldAutoReconnect() || connected) return@Runnable
        connectInternal(autoReconnectAddress, autoReconnectName, true)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== bluetoothGatt) { closeGattIfPermitted(bluetoothGatt); return }
            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_DISCONNECTED) {
                failConnection(ConnectionState.CONNECTION_FAILED, getString(R.string.status_connection_failed, status))
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (!hasConnectPermission()) {
                    failConnection(ConnectionState.PERMISSION_REQUIRED, getString(R.string.status_auto_connect_permission_required))
                    return
                }
                connected = true
                reconnectAttempts = 0
                updateState { it.withConnectionState(ConnectionState.DISCOVERING_SERVICES, true, getString(R.string.status_connected_discovering)) }
                if (!bluetoothGatt.discoverServices()) {
                    failConnection(ConnectionState.SERVICE_DISCOVERY_FAILED, getString(R.string.status_service_discovery_start_failed))
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false; writeInFlight = false; currentCommand = null; commandQueue.clear(); writeCharacteristic = null
                handler.removeCallbacks(pollRunnable); handler.removeCallbacks(commandTimeoutRunnable); frameAssembler.reset(); gatt = null
                closeGattIfPermitted(bluetoothGatt)
                if (!intentionalDisconnect && shouldAutoReconnect()) {
                    scheduleReconnect(getString(R.string.status_connection_lost))
                } else if (!intentionalDisconnect) {
                    publishDisconnected(ConnectionState.CONNECTION_FAILED, getString(R.string.status_connection_lost), true)
                } else {
                    publishDisconnected(ConnectionState.DISCONNECTED, getString(R.string.status_select_bms))
                }
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(ConnectionState.SERVICE_DISCOVERY_FAILED, getString(R.string.status_service_discovery_failed, status))
                return
            }
            val service = bluetoothGatt.getService(BleConstants.SERVICE_UUID) ?: run {
                failConnection(ConnectionState.SERVICE_NOT_FOUND, getString(R.string.status_jbd_service_not_found)); return
            }
            writeCharacteristic = service.getCharacteristic(BleConstants.WRITE_UUID)
            val notifyCharacteristic = service.getCharacteristic(BleConstants.NOTIFY_UUID)
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                failConnection(ConnectionState.CHARACTERISTICS_NOT_FOUND, getString(R.string.status_jbd_characteristics_not_found)); return
            }
            updateState { it.withConnectionState(ConnectionState.ENABLING_NOTIFICATIONS, true, getString(R.string.status_enabling_notifications)) }
            if (!enableNotifications(bluetoothGatt, notifyCharacteristic)) {
                failConnection(ConnectionState.NOTIFICATIONS_FAILED, getString(R.string.status_notifications_failed))
            }
        }

        override fun onCharacteristicChanged(bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) = handleIncoming(characteristic.value)

        override fun onCharacteristicChanged(bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) = handleIncoming(value)

        override fun onCharacteristicWrite(bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            writeInFlight = false
            if (status == BluetoothGatt.GATT_SUCCESS && currentCommand != null) { handler.removeCallbacks(commandTimeoutRunnable); handler.postDelayed(commandTimeoutRunnable, COMMAND_TIMEOUT_MS) }
            else { completeCurrentCommand() }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(bluetoothGatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (gatt !== bluetoothGatt) { closeGattIfPermitted(bluetoothGatt); return }
            if (status == BluetoothGatt.GATT_SUCCESS) startReading()
            else failConnection(ConnectionState.NOTIFICATIONS_FAILED, getString(R.string.status_notifications_failed))
        }
    }

    init {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        adapter = manager?.adapter
    }

    fun refreshLocalizedStatus() {
        updateState { it.withConnectionState(it.connectionState, it.connected, localizedStatus(it)) }
    }

    fun isConnected(): Boolean = connected

    fun connectedDeviceName(): String? = connectedDeviceName

    fun setRefreshInterval(intervalMs: Long) {
        refreshIntervalMs = maxOf(1000L, intervalMs)
        if (connected) { handler.removeCallbacks(pollRunnable); handler.post(pollRunnable) }
    }

    fun setAutoReconnect(enabled: Boolean, address: String?, name: String?) {
        autoReconnectEnabled = enabled; autoReconnectAddress = address; autoReconnectName = if (name.isNullOrEmpty()) address else name
        if (!enabled) { handler.removeCallbacks(reconnectRunnable); reconnectScheduled = false; reconnectAttempts = 0 }
    }

    fun connect(address: String, name: String?) = connectInternal(address, name, false)

    @SuppressLint("MissingPermission")
    private fun connectInternal(address: String?, name: String?, reconnect: Boolean) {
        handler.removeCallbacks(reconnectRunnable); reconnectScheduled = false; intentionalDisconnect = false
        if (!reconnect) { reconnectAttempts = 0; extendedInfoRequested = false }
        if (adapter == null) { handleConnectPrecheckFailure(ConnectionState.BLUETOOTH_UNAVAILABLE, getString(R.string.status_bluetooth_unavailable), reconnect); return }
        if (!hasConnectPermission()) { handleConnectPrecheckFailure(ConnectionState.PERMISSION_REQUIRED, getString(R.string.status_auto_connect_permission_required), reconnect); return }
        if (!adapter!!.isEnabled) { handleConnectPrecheckFailure(ConnectionState.BLUETOOTH_OFF, getString(if (reconnect) R.string.status_auto_connect_bluetooth_off else R.string.status_bluetooth_off), reconnect); return }
        disconnectInternal(false)
        connectedDeviceAddress = address; connectedDeviceName = if (name.isNullOrEmpty()) address else name
        updateState(BmsUiState.withConnectionState(ConnectionState.CONNECTING, false, connectedDeviceName, connectedDeviceAddress, getString(R.string.status_connecting, connectedDeviceName), null, null))
        try { val device = adapter!!.getRemoteDevice(address!!); gatt = device.connectGatt(context, false, gattCallback) }
        catch (_: IllegalArgumentException) { publishDisconnected(ConnectionState.INVALID_DEVICE, getString(R.string.status_auto_connect_invalid_device), true) }
    }

    fun disconnect() = disconnectInternal(true)

    fun cancelReconnect() {
        handler.removeCallbacks(reconnectRunnable)
        if (!reconnectScheduled && repository.getSnapshot().connectionState != ConnectionState.WAITING_RECONNECT) return
        intentionalDisconnect = true; reconnectScheduled = false; reconnectAttempts = 0
        publishDisconnected(ConnectionState.DISCONNECTED, getString(R.string.status_reconnect_cancelled))
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal(publishState: Boolean) {
        if (publishState) intentionalDisconnect = true
        handler.removeCallbacks(reconnectRunnable); reconnectScheduled = false; handler.removeCallbacks(pollRunnable)
        connected = false; writeInFlight = false; currentCommand = null; commandQueue.clear(); writeCharacteristic = null
        handler.removeCallbacks(commandTimeoutRunnable); frameAssembler.reset()
        if (gatt != null) { if (hasConnectPermission()) { gatt!!.disconnect(); gatt!!.close() }; gatt = null }
        if (publishState) publishDisconnected(ConnectionState.DISCONNECTED, getString(R.string.status_select_bms))
    }

    private fun hasConnectPermission(): Boolean = Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun closeGattIfPermitted(bluetoothGatt: BluetoothGatt) { if (hasConnectPermission()) bluetoothGatt.close() }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic): Boolean {
        if (!hasConnectPermission()) return false
        if (!bluetoothGatt.setCharacteristicNotification(characteristic, true)) return false
        val descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CONFIG_UUID) ?: return false
        if (Build.VERSION.SDK_INT >= 33) return bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        return bluetoothGatt.writeDescriptor(descriptor)
    }

    private fun startReading() {
        updateState { it.withConnectionState(ConnectionState.READY, true, getString(R.string.status_ready_reading)) }
        handler.removeCallbacks(pollRunnable); handler.postDelayed(pollRunnable, 500L)
    }

    private fun enqueuePollCycle() {
        commandQueue.addLast(CommandRequest.read(JbdCommands.CMD_BASIC_INFO, CommandKind.BASIC_INFO))
        commandQueue.addLast(CommandRequest.read(JbdCommands.CMD_CELL_VOLTAGE, CommandKind.CELL_VOLTAGES))
        if (!extendedInfoRequested) {
            extendedInfoRequested = true
            commandQueue.addLast(CommandRequest(JbdCommands.CMD_FACTORY_MODE, CommandKind.FACTORY_MODE, JbdCommands.openFactoryMode()))
            commandQueue.addLast(CommandRequest.read(JbdCommands.CMD_SERIAL_NUMBER, CommandKind.SERIAL_NUMBER))
            commandQueue.addLast(CommandRequest.read(JbdCommands.CMD_BARCODE, CommandKind.BARCODE))
            commandQueue.addLast(CommandRequest.read(JbdCommands.CMD_MANUFACTURER, CommandKind.MANUFACTURER))
            commandQueue.addLast(CommandRequest.read(JbdCommands.CMD_BATTERY_MODEL, CommandKind.BATTERY_MODEL))
            commandQueue.addLast(CommandRequest(JbdCommands.CMD_EXTENDED_PARAMS, CommandKind.EXT_RATINGS, JbdCommands.readExtendedParams(EXT_RATINGS_START, 8)))
            commandQueue.addLast(CommandRequest(JbdCommands.CMD_EXTENDED_PARAMS, CommandKind.EXT_BMS_ADDRESS, JbdCommands.readExtendedParams(EXT_BMS_ADDRESS_START, 6)))
            commandQueue.addLast(CommandRequest(JbdCommands.CMD_EXTENDED_PARAMS, CommandKind.EXT_BMS_MODEL, JbdCommands.readExtendedParams(EXT_BMS_MODEL_START, 8)))
            commandQueue.addLast(CommandRequest(JbdCommands.CMD_CLOSE_FACTORY_MODE, CommandKind.CLOSE_FACTORY_MODE, JbdCommands.closeFactoryMode()))
        }
    }

    private fun drainCommandQueue() {
        if (!connected || writeInFlight || currentCommand != null) return
        val request = commandQueue.pollFirst() ?: run { handler.removeCallbacks(pollRunnable); handler.postDelayed(pollRunnable, refreshIntervalMs); return }
        sendCommand(request)
    }

    @SuppressLint("MissingPermission")
    private fun sendCommand(request: CommandRequest) {
        if (gatt == null || writeCharacteristic == null || writeInFlight || currentCommand != null || !hasConnectPermission()) return
        currentCommand = request; writeInFlight = true
        val g = gatt!!; val wc = writeCharacteristic!!
        val started = if (Build.VERSION.SDK_INT >= 33) g.writeCharacteristic(wc, request.command, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
        else { @Suppress("DEPRECATION") wc.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT; wc.value = request.command; g.writeCharacteristic(wc) }
        if (!started) { writeInFlight = false; currentCommand = null; drainCommandQueue() }
    }

    private fun handleIncoming(value: ByteArray) {
        val frames = frameAssembler.append(value)
        if (frames.isEmpty()) return
        handler.post {
            for (frame in frames) {
                try {
                    when (frame.command) {
                        JbdCommands.CMD_BASIC_INFO.toInt() and 0xFF -> {
                            val info = JbdParser.parseBasicInfo(frame)
                            updateState { it.withBasicInfo(info, getString(R.string.status_ready_reading)) }
                        }
                        JbdCommands.CMD_CELL_VOLTAGE.toInt() and 0xFF -> {
                            val voltages = JbdParser.parseCellVoltages(frame)
                            updateState { it.withCellVoltages(voltages, it.status) }
                        }
                        else -> handleDeviceInfoFrame(frame)
                    }
                    val respCmd = currentCommand?.responseCommand?.toInt()?.and(0xFF)
                    if (respCmd != null && frame.command == respCmd) completeCurrentCommand()
                } catch (e: JbdParseException) {
                    updateState { it.withConnectionState(ConnectionState.PARSE_ERROR, connected, getString(R.string.status_parse_error, e.message)) }
                    val respCmd2 = currentCommand?.responseCommand?.toInt()?.and(0xFF)
                    if (respCmd2 != null && frame.command == respCmd2) completeCurrentCommand()
                }
            }
        }
    }

    @Throws(JbdParseException::class)
    private fun handleDeviceInfoFrame(frame: JbdFrame) {
        val cmd = currentCommand ?: return
        if (frame.command != (cmd.responseCommand.toInt() and 0xFF)) return
        updateState { snapshot ->
            var info: JbdDeviceInfo = snapshot.deviceInfo ?: JbdDeviceInfo.EMPTY
            when (cmd.kind) {
                CommandKind.SERIAL_NUMBER -> info = info.withSerialNumber(JbdParser.parseSerialNumber(frame))
                CommandKind.BARCODE -> info = info.withBarcode(JbdParser.parseText(frame))
                CommandKind.MANUFACTURER -> info = info.withManufacturer(JbdParser.parseText(frame))
                CommandKind.BATTERY_MODEL -> info = info.withBatteryModel(JbdParser.parseText(frame))
                CommandKind.EXT_RATINGS, CommandKind.EXT_BMS_ADDRESS, CommandKind.EXT_BMS_MODEL ->
                    info = applyExtendedParams(info, JbdParser.parseExtendedParams(frame))
                else -> {}
            }
            if (info.hasAnyField()) snapshot.withDeviceInfo(info, snapshot.status) else snapshot
        }
    }

    private fun applyExtendedParams(info: JbdDeviceInfo, params: JbdParser.ExtendedParams): JbdDeviceInfo {
        if (params.start == EXT_RATINGS_START && params.data.size >= 8) {
            val dischargeCurrent = JbdParser.u16(params.data, 2).toFloat()
            val chargeCurrent = JbdParser.u16(params.data, 4).toFloat()
            val dischargePower = JbdParser.u16(params.data, 6).toFloat()
            return info.withRatings(chargeCurrent, dischargeCurrent, dischargePower)
        }
        if (params.start == EXT_BMS_ADDRESS_START) return info.withBmsAddress(JbdParser.hexFromBytes(params.data))
        if (params.start == EXT_BMS_MODEL_START) return info.withBmsModel(JbdParser.textFromBytes(params.data))
        return info
    }

    private fun completeCurrentCommand() { handler.removeCallbacks(commandTimeoutRunnable); currentCommand = null; writeInFlight = false; drainCommandQueue() }

    private fun failConnection(state: ConnectionState, message: String) {
        disconnectInternal(false)
        if (shouldAutoReconnect()) scheduleReconnect(message) else publishDisconnected(state, message, true)
    }

    private fun publishDisconnected(state: ConnectionState, status: String) = publishDisconnected(state, status, false)

    private fun publishDisconnected(state: ConnectionState, status: String, keepDevice: Boolean) {
        val deviceName = if (keepDevice) connectedDeviceName else null
        val deviceAddress = if (keepDevice) connectedDeviceAddress else null
        if (!keepDevice) { connectedDeviceName = null; connectedDeviceAddress = null }
        updateState(BmsUiState.withConnectionState(state, false, deviceName, deviceAddress, status, null, null))
    }

    private fun handleConnectPrecheckFailure(state: ConnectionState, status: String, reconnect: Boolean) {
        if (reconnect && shouldAutoReconnect()) scheduleReconnect(status) else publishDisconnected(state, status)
    }

    private fun shouldAutoReconnect(): Boolean = autoReconnectEnabled && !autoReconnectAddress.isNullOrEmpty()

    private fun scheduleReconnect(reason: String) {
        if (reconnectScheduled) return
        reconnectAttempts++
        val delayMs = minOf(AUTO_RECONNECT_MAX_DELAY_MS, AUTO_RECONNECT_BASE_DELAY_MS * reconnectAttempts)
        reconnectScheduled = true
        connectedDeviceAddress = autoReconnectAddress
        connectedDeviceName = if (autoReconnectName.isNullOrEmpty()) autoReconnectAddress else autoReconnectName
        updateState(BmsUiState.withConnectionState(ConnectionState.WAITING_RECONNECT, false, connectedDeviceName, connectedDeviceAddress, getString(R.string.status_reconnecting, reason, delayMs / 1000L), null, null))
        handler.postDelayed(reconnectRunnable, delayMs)
    }

    private fun getString(resId: Int, vararg args: Any?): String = AppSettings.preferredContext(context).getString(resId, *args)

    private fun localizedStatus(snapshot: BmsUiState): String = when (snapshot.connectionState) {
        ConnectionState.DISCONNECTED -> getString(R.string.status_select_bms)
        ConnectionState.CONNECTING -> { if (!snapshot.deviceName.isNullOrEmpty()) getString(R.string.status_connecting, snapshot.deviceName) else snapshot.status ?: getString(R.string.status_connecting, "") }
        ConnectionState.DISCOVERING_SERVICES -> getString(R.string.status_connected_discovering)
        ConnectionState.ENABLING_NOTIFICATIONS -> getString(R.string.status_enabling_notifications)
        ConnectionState.READING, ConnectionState.READY -> getString(R.string.status_ready_reading)
        ConnectionState.BLUETOOTH_UNAVAILABLE -> getString(R.string.status_bluetooth_unavailable)
        ConnectionState.BLUETOOTH_OFF -> getString(R.string.status_bluetooth_off)
        ConnectionState.PERMISSION_REQUIRED -> getString(R.string.status_auto_connect_permission_required)
        ConnectionState.INVALID_DEVICE -> getString(R.string.status_auto_connect_invalid_device)
        ConnectionState.SERVICE_DISCOVERY_FAILED -> getString(R.string.status_service_discovery_start_failed)
        ConnectionState.SERVICE_NOT_FOUND -> getString(R.string.status_jbd_service_not_found)
        ConnectionState.CHARACTERISTICS_NOT_FOUND -> getString(R.string.status_jbd_characteristics_not_found)
        ConnectionState.NOTIFICATIONS_FAILED -> getString(R.string.status_notifications_failed)
        else -> snapshot.status ?: ""
    }

    /**
     * Applies a state change, serializing the read-modify-write on the main looper.
     * GATT callbacks run on binder threads (no Handler was passed to connectGatt), so posting here
     * keeps StateFlow writers single-threaded and prevents a stale snapshot read from overwriting
     * a newer state written in between.
     */
    private fun updateState(snapshot: BmsUiState) {
        if (Looper.myLooper() == Looper.getMainLooper()) repository.update(snapshot)
        else handler.post { repository.update(snapshot) }
    }

    private fun updateState(transform: (BmsUiState) -> BmsUiState) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            repository.update(transform(repository.getSnapshot()))
        } else {
            handler.post { repository.update(transform(repository.getSnapshot())) }
        }
    }

    private enum class CommandKind { BASIC_INFO, CELL_VOLTAGES, FACTORY_MODE, CLOSE_FACTORY_MODE, SERIAL_NUMBER, BARCODE, MANUFACTURER, BATTERY_MODEL, EXT_RATINGS, EXT_BMS_ADDRESS, EXT_BMS_MODEL }

    private class CommandRequest(val responseCommand: Byte, val kind: CommandKind, val command: ByteArray) {
        companion object { fun read(command: Byte, kind: CommandKind) = CommandRequest(command, kind, JbdCommands.readCommand(command)) }
    }

    companion object {
        private const val AUTO_RECONNECT_BASE_DELAY_MS = 5000L
        private const val AUTO_RECONNECT_MAX_DELAY_MS = 30000L
        private const val COMMAND_TIMEOUT_MS = 2200L
        private const val EXT_RATINGS_START = 117
        private const val EXT_BMS_ADDRESS_START = 170
        private const val EXT_BMS_MODEL_START = 176
    }
}
