package com.gytxtx.openjbd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.gytxtx.openjbd.ble.BleConstants;
import com.gytxtx.openjbd.protocol.JbdBasicInfo;
import com.gytxtx.openjbd.protocol.JbdCellVoltages;
import com.gytxtx.openjbd.protocol.JbdCommands;
import com.gytxtx.openjbd.protocol.JbdFrame;
import com.gytxtx.openjbd.protocol.JbdFrameAssembler;
import com.gytxtx.openjbd.protocol.JbdParseException;
import com.gytxtx.openjbd.protocol.JbdParser;

import java.util.ArrayDeque;
import java.util.List;

final class BmsConnectionManager {
    private static BmsConnectionManager instance;
    private static final long AUTO_RECONNECT_BASE_DELAY_MS = 5000L;
    private static final long AUTO_RECONNECT_MAX_DELAY_MS = 30000L;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final JbdFrameAssembler frameAssembler = new JbdFrameAssembler();
    private final ArrayDeque<byte[]> commandQueue = new ArrayDeque<>();

    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private boolean connected;
    private boolean writeInFlight;
    private String connectedDeviceName;
    private String connectedDeviceAddress;
    private boolean autoReconnectEnabled;
    private boolean intentionalDisconnect;
    private boolean reconnectScheduled;
    private String autoReconnectAddress;
    private String autoReconnectName;
    private int reconnectAttempts;
    private long refreshIntervalMs = 2000L;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!connected) {
                return;
            }
            sendCommand(JbdCommands.readBasicInfo());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (connected) {
                        sendCommand(JbdCommands.readCellVoltage());
                    }
                }
            }, 250L);
            handler.postDelayed(this, refreshIntervalMs);
        }
    };

    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            reconnectScheduled = false;
            if (!shouldAutoReconnect() || connected) {
                return;
            }
            connectInternal(autoReconnectAddress, autoReconnectName, true);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            if (gatt != bluetoothGatt) {
                bluetoothGatt.close();
                return;
            }
            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_DISCONNECTED) {
                failConnection(BmsStateStore.ConnectionState.CONNECTION_FAILED, context.getString(R.string.status_connection_failed, status));
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                reconnectAttempts = 0;
                updateState(BmsStateStore.getSnapshot().withConnectionState(BmsStateStore.ConnectionState.DISCOVERING_SERVICES, true, context.getString(R.string.status_connected_discovering)));
                if (!bluetoothGatt.discoverServices()) {
                    failConnection(BmsStateStore.ConnectionState.SERVICE_DISCOVERY_FAILED, context.getString(R.string.status_service_discovery_start_failed));
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false;
                writeInFlight = false;
                commandQueue.clear();
                writeCharacteristic = null;
                handler.removeCallbacks(pollRunnable);
                frameAssembler.reset();
                gatt = null;
                bluetoothGatt.close();
                if (!intentionalDisconnect && shouldAutoReconnect()) {
                    scheduleReconnect(context.getString(R.string.status_connection_lost));
                } else {
                    publishDisconnected(BmsStateStore.ConnectionState.DISCONNECTED, context.getString(R.string.status_select_bms));
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(BmsStateStore.ConnectionState.SERVICE_DISCOVERY_FAILED, context.getString(R.string.status_service_discovery_failed, status));
                return;
            }
            BluetoothGattService service = bluetoothGatt.getService(BleConstants.SERVICE_UUID);
            if (service == null) {
                failConnection(BmsStateStore.ConnectionState.SERVICE_NOT_FOUND, context.getString(R.string.status_jbd_service_not_found));
                return;
            }
            writeCharacteristic = service.getCharacteristic(BleConstants.WRITE_UUID);
            BluetoothGattCharacteristic notifyCharacteristic = service.getCharacteristic(BleConstants.NOTIFY_UUID);
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                failConnection(BmsStateStore.ConnectionState.CHARACTERISTICS_NOT_FOUND, context.getString(R.string.status_jbd_characteristics_not_found));
                return;
            }
            updateState(BmsStateStore.getSnapshot().withConnectionState(BmsStateStore.ConnectionState.ENABLING_NOTIFICATIONS, true, context.getString(R.string.status_enabling_notifications)));
            if (!enableNotifications(bluetoothGatt, notifyCharacteristic)) {
                failConnection(BmsStateStore.ConnectionState.NOTIFICATIONS_FAILED, context.getString(R.string.status_notifications_failed));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
            handleIncoming(characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            handleIncoming(value);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, int status) {
            writeInFlight = false;
            if (!commandQueue.isEmpty() && connected) {
                sendCommand(commandQueue.removeFirst());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor descriptor, int status) {
            if (gatt != bluetoothGatt) {
                bluetoothGatt.close();
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                startReading();
            } else {
                failConnection(BmsStateStore.ConnectionState.NOTIFICATIONS_FAILED, context.getString(R.string.status_notifications_failed));
            }
        }
    };

    private BmsConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager manager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
    }

    static synchronized BmsConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new BmsConnectionManager(context);
        }
        return instance;
    }

    boolean isConnected() {
        return connected;
    }

    String connectedDeviceName() {
        return connectedDeviceName;
    }

    void setRefreshInterval(long intervalMs) {
        refreshIntervalMs = Math.max(1000L, intervalMs);
        if (connected) {
            handler.removeCallbacks(pollRunnable);
            handler.post(pollRunnable);
        }
    }

    void setAutoReconnect(boolean enabled, String address, String name) {
        autoReconnectEnabled = enabled;
        autoReconnectAddress = address;
        autoReconnectName = name == null || name.length() == 0 ? address : name;
        if (!enabled) {
            handler.removeCallbacks(reconnectRunnable);
            reconnectScheduled = false;
            reconnectAttempts = 0;
        }
    }

    @SuppressLint("MissingPermission")
    void connect(String address, String name) {
        connectInternal(address, name, false);
    }

    @SuppressLint("MissingPermission")
    private void connectInternal(String address, String name, boolean reconnect) {
        handler.removeCallbacks(reconnectRunnable);
        reconnectScheduled = false;
        intentionalDisconnect = false;
        if (!reconnect) {
            reconnectAttempts = 0;
        }
        if (adapter == null) {
            handleConnectPrecheckFailure(BmsStateStore.ConnectionState.BLUETOOTH_UNAVAILABLE, context.getString(R.string.status_bluetooth_unavailable), reconnect);
            return;
        }
        if (!adapter.isEnabled()) {
            handleConnectPrecheckFailure(BmsStateStore.ConnectionState.BLUETOOTH_OFF, context.getString(reconnect ? R.string.status_auto_connect_bluetooth_off : R.string.status_bluetooth_off), reconnect);
            return;
        }
        if (!hasConnectPermission()) {
            handleConnectPrecheckFailure(BmsStateStore.ConnectionState.PERMISSION_REQUIRED, context.getString(R.string.status_auto_connect_permission_required), reconnect);
            return;
        }
        disconnect(false);
        connectedDeviceAddress = address;
        connectedDeviceName = name == null || name.length() == 0 ? address : name;
        updateState(BmsStateStore.Snapshot.withConnectionState(BmsStateStore.ConnectionState.CONNECTING, false, connectedDeviceName, connectedDeviceAddress, context.getString(R.string.status_connecting, connectedDeviceName), null, null));
        try {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            gatt = device.connectGatt(context, false, gattCallback);
        } catch (IllegalArgumentException ignored) {
            publishDisconnected(BmsStateStore.ConnectionState.INVALID_DEVICE, context.getString(R.string.status_auto_connect_invalid_device));
        }
    }

    void disconnect() {
        disconnect(true);
    }

    @SuppressLint("MissingPermission")
    private void disconnect(boolean publishState) {
        if (publishState) {
            intentionalDisconnect = true;
        }
        handler.removeCallbacks(reconnectRunnable);
        reconnectScheduled = false;
        handler.removeCallbacks(pollRunnable);
        connected = false;
        writeInFlight = false;
        commandQueue.clear();
        writeCharacteristic = null;
        frameAssembler.reset();
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        if (publishState) {
            publishDisconnected(BmsStateStore.ConnectionState.DISCONNECTED, context.getString(R.string.status_select_bms));
        }
    }

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private boolean enableNotifications(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothGatt.setCharacteristicNotification(characteristic, true)) {
            return false;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CONFIG_UUID);
        if (descriptor == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothGatt.GATT_SUCCESS;
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        return bluetoothGatt.writeDescriptor(descriptor);
    }

    private void startReading() {
        updateState(BmsStateStore.getSnapshot().withConnectionState(BmsStateStore.ConnectionState.READY, true, context.getString(R.string.status_ready_reading)));
        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, 500L);
    }

    @SuppressLint("MissingPermission")
    private void sendCommand(byte[] command) {
        if (gatt == null || writeCharacteristic == null || writeInFlight) {
            if (connected && writeInFlight && commandQueue.size() < 4) {
                commandQueue.addLast(command);
            }
            return;
        }
        writeInFlight = true;
        boolean started;
        if (Build.VERSION.SDK_INT >= 33) {
            started = gatt.writeCharacteristic(writeCharacteristic, command, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS;
        } else {
            writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            writeCharacteristic.setValue(command);
            started = gatt.writeCharacteristic(writeCharacteristic);
        }
        if (!started) {
            writeInFlight = false;
        }
    }

    private void handleIncoming(byte[] value) {
        final List<JbdFrame> frames = frameAssembler.append(value);
        if (frames.isEmpty()) {
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (JbdFrame frame : frames) {
                    try {
                        if (frame.command == (JbdCommands.CMD_BASIC_INFO & 0xFF)) {
                            JbdBasicInfo info = JbdParser.parseBasicInfo(frame);
                            BmsStateStore.Snapshot current = BmsStateStore.getSnapshot();
                            updateState(current.withBasicInfo(info, context.getString(R.string.status_ready_reading)));
                        } else if (frame.command == (JbdCommands.CMD_CELL_VOLTAGE & 0xFF)) {
                            JbdCellVoltages voltages = JbdParser.parseCellVoltages(frame);
                            BmsStateStore.Snapshot current = BmsStateStore.getSnapshot();
                            updateState(current.withCellVoltages(voltages, current.status));
                        }
                    } catch (JbdParseException e) {
                        BmsStateStore.Snapshot current = BmsStateStore.getSnapshot();
                        updateState(current.withConnectionState(BmsStateStore.ConnectionState.PARSE_ERROR, connected, context.getString(R.string.status_parse_error, e.getMessage())));
                    }
                }
            }
        });
    }

    private void failConnection(BmsStateStore.ConnectionState state, String message) {
        disconnect(false);
        if (shouldAutoReconnect()) {
            scheduleReconnect(message);
        } else {
            publishDisconnected(state, message);
        }
    }

    private void publishDisconnected(BmsStateStore.ConnectionState state, String status) {
        connectedDeviceName = null;
        connectedDeviceAddress = null;
        BmsDashboardStore.update(null);
        updateState(BmsStateStore.Snapshot.withConnectionState(state, false, null, null, status, null, null));
    }

    private void handleConnectPrecheckFailure(BmsStateStore.ConnectionState state, String status, boolean reconnect) {
        if (reconnect && shouldAutoReconnect()) {
            scheduleReconnect(status);
        } else {
            publishDisconnected(state, status);
        }
    }

    private boolean shouldAutoReconnect() {
        return autoReconnectEnabled && autoReconnectAddress != null && autoReconnectAddress.length() > 0;
    }

    private void scheduleReconnect(String reason) {
        if (reconnectScheduled) {
            return;
        }
        reconnectAttempts++;
        long delayMs = Math.min(AUTO_RECONNECT_MAX_DELAY_MS, AUTO_RECONNECT_BASE_DELAY_MS * reconnectAttempts);
        reconnectScheduled = true;
        connectedDeviceAddress = autoReconnectAddress;
        connectedDeviceName = autoReconnectName == null || autoReconnectName.length() == 0 ? autoReconnectAddress : autoReconnectName;
        BmsDashboardStore.update(null);
        updateState(BmsStateStore.Snapshot.withConnectionState(
                BmsStateStore.ConnectionState.WAITING_RECONNECT,
                false,
                connectedDeviceName,
                connectedDeviceAddress,
                context.getString(R.string.status_reconnecting, reason, delayMs / 1000L),
                null,
                null));
        handler.postDelayed(reconnectRunnable, delayMs);
    }

    private void updateState(BmsStateStore.Snapshot snapshot) {
        BmsStateStore.update(snapshot);
        if (snapshot.basicInfo != null) {
            BmsDashboardStore.update(new BmsDashboardStore.Snapshot(snapshot.basicInfo.soc, snapshot.basicInfo.totalVoltage, snapshot.basicInfo.current, snapshot.basicInfo.totalVoltage * snapshot.basicInfo.current));
        }
    }
}
