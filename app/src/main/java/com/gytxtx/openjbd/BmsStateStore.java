package com.gytxtx.openjbd;

import android.os.Handler;
import android.os.Looper;

import com.gytxtx.openjbd.protocol.JbdBasicInfo;
import com.gytxtx.openjbd.protocol.JbdCellVoltages;
import com.gytxtx.openjbd.protocol.JbdDeviceInfo;

import java.util.ArrayList;
import java.util.List;

final class BmsStateStore {
    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static Snapshot snapshot = Snapshot.disconnected(null, null, "");

    private BmsStateStore() {
    }

    static synchronized Snapshot getSnapshot() {
        return snapshot;
    }

    static void update(Snapshot value) {
        List<Listener> listeners;
        synchronized (BmsStateStore.class) {
            snapshot = value;
            listeners = new ArrayList<>(LISTENERS);
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifyListeners(listeners, value);
        } else {
            MAIN_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    notifyListeners(listeners, value);
                }
            });
        }
    }

    private static void notifyListeners(List<Listener> listeners, Snapshot value) {
        for (Listener listener : listeners) {
            listener.onBmsStateChanged(value);
        }
    }

    static synchronized void addListener(Listener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    static synchronized void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    interface Listener {
        void onBmsStateChanged(Snapshot snapshot);
    }

    static final class Snapshot {
        final boolean connected;
        final ConnectionState connectionState;
        final String deviceName;
        final String deviceAddress;
        final String status;
        final JbdBasicInfo basicInfo;
        final JbdCellVoltages cellVoltages;
        final JbdDeviceInfo deviceInfo;
        final long updatedAtMillis;

        Snapshot(boolean connected, String deviceName, String deviceAddress, String status, JbdBasicInfo basicInfo, JbdCellVoltages cellVoltages, long updatedAtMillis) {
            this(connected, connected ? ConnectionState.READY : ConnectionState.DISCONNECTED, deviceName, deviceAddress, status, basicInfo, cellVoltages, null, updatedAtMillis);
        }

        Snapshot(boolean connected, ConnectionState connectionState, String deviceName, String deviceAddress, String status, JbdBasicInfo basicInfo, JbdCellVoltages cellVoltages, JbdDeviceInfo deviceInfo, long updatedAtMillis) {
            this.connected = connected;
            this.connectionState = connectionState;
            this.deviceName = deviceName;
            this.deviceAddress = deviceAddress;
            this.status = status;
            this.basicInfo = basicInfo;
            this.cellVoltages = cellVoltages;
            this.deviceInfo = deviceInfo;
            this.updatedAtMillis = updatedAtMillis;
        }

        static Snapshot disconnected(String deviceName, String deviceAddress, String status) {
            return withConnectionState(ConnectionState.DISCONNECTED, false, deviceName, deviceAddress, status, null, null);
        }

        static Snapshot withConnectionState(ConnectionState connectionState, boolean connected, String deviceName, String deviceAddress, String status, JbdBasicInfo basicInfo, JbdCellVoltages cellVoltages) {
            return new Snapshot(connected, connectionState, deviceName, deviceAddress, status, basicInfo, cellVoltages, null, System.currentTimeMillis());
        }

        Snapshot withStatus(boolean connected, String status) {
            return withConnectionState(connected ? ConnectionState.READY : ConnectionState.DISCONNECTED, connected, status);
        }

        Snapshot withConnectionState(ConnectionState connectionState, boolean connected, String status) {
            return new Snapshot(connected, connectionState, deviceName, deviceAddress, status, basicInfo, cellVoltages, deviceInfo, System.currentTimeMillis());
        }

        Snapshot withBasicInfo(JbdBasicInfo info, String status) {
            return new Snapshot(connected, ConnectionState.READING, deviceName, deviceAddress, status, info, cellVoltages, deviceInfo, System.currentTimeMillis());
        }

        Snapshot withCellVoltages(JbdCellVoltages voltages, String status) {
            return new Snapshot(connected, connectionState, deviceName, deviceAddress, status, basicInfo, voltages, deviceInfo, System.currentTimeMillis());
        }

        Snapshot withDeviceInfo(JbdDeviceInfo info, String status) {
            return new Snapshot(connected, connectionState, deviceName, deviceAddress, status, basicInfo, cellVoltages, info, System.currentTimeMillis());
        }
    }

    enum ConnectionState {
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
