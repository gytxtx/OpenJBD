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

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                updateState(BmsStateStore.getSnapshot().withStatus(true, context.getString(R.string.status_connected_discovering)));
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false;
                writeInFlight = false;
                commandQueue.clear();
                writeCharacteristic = null;
                handler.removeCallbacks(pollRunnable);
                frameAssembler.reset();
                updateState(BmsStateStore.Snapshot.disconnected(null, null, context.getString(R.string.status_select_bms)));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            BluetoothGattService service = bluetoothGatt.getService(BleConstants.SERVICE_UUID);
            if (service == null) {
                failConnection(context.getString(R.string.status_jbd_service_not_found));
                return;
            }
            writeCharacteristic = service.getCharacteristic(BleConstants.WRITE_UUID);
            BluetoothGattCharacteristic notifyCharacteristic = service.getCharacteristic(BleConstants.NOTIFY_UUID);
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                failConnection(context.getString(R.string.status_jbd_characteristics_not_found));
                return;
            }
            enableNotifications(bluetoothGatt, notifyCharacteristic);
            updateState(BmsStateStore.getSnapshot().withStatus(true, context.getString(R.string.status_ready_reading)));
            handler.removeCallbacks(pollRunnable);
            handler.postDelayed(pollRunnable, 500L);
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

    @SuppressLint("MissingPermission")
    void connect(String address, String name) {
        if (adapter == null) {
            publishDisconnected(context.getString(R.string.status_bluetooth_unavailable));
            return;
        }
        if (!adapter.isEnabled()) {
            publishDisconnected(context.getString(R.string.status_bluetooth_off));
            return;
        }
        if (!hasConnectPermission()) {
            publishDisconnected(context.getString(R.string.status_auto_connect_permission_required));
            return;
        }
        disconnect(false);
        connectedDeviceAddress = address;
        connectedDeviceName = name == null || name.length() == 0 ? address : name;
        updateState(new BmsStateStore.Snapshot(false, connectedDeviceName, connectedDeviceAddress, context.getString(R.string.status_connecting, connectedDeviceName), null, null, System.currentTimeMillis()));
        try {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            gatt = device.connectGatt(context, false, gattCallback);
        } catch (IllegalArgumentException ignored) {
            publishDisconnected(context.getString(R.string.status_auto_connect_invalid_device));
        }
    }

    void disconnect() {
        disconnect(true);
    }

    @SuppressLint("MissingPermission")
    private void disconnect(boolean publishState) {
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
            publishDisconnected(context.getString(R.string.status_select_bms));
        }
    }

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
        bluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CONFIG_UUID);
        if (descriptor == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
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
                        updateState(current.withStatus(connected, context.getString(R.string.status_parse_error, e.getMessage())));
                    }
                }
            }
        });
    }

    private void failConnection(String message) {
        disconnect(false);
        publishDisconnected(message);
    }

    private void publishDisconnected(String status) {
        connectedDeviceName = null;
        connectedDeviceAddress = null;
        BmsDashboardStore.update(null);
        updateState(BmsStateStore.Snapshot.disconnected(null, null, status));
    }

    private void updateState(BmsStateStore.Snapshot snapshot) {
        BmsStateStore.update(snapshot);
        if (snapshot.basicInfo != null) {
            BmsDashboardStore.update(new BmsDashboardStore.Snapshot(snapshot.basicInfo.soc, snapshot.basicInfo.totalVoltage, snapshot.basicInfo.current, snapshot.basicInfo.totalVoltage * snapshot.basicInfo.current));
        }
    }
}
