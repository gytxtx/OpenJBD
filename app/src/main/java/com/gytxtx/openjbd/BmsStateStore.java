package com.gytxtx.openjbd;

import android.os.Handler;
import android.os.Looper;

import com.gytxtx.openjbd.protocol.JbdBasicInfo;
import com.gytxtx.openjbd.protocol.JbdCellVoltages;

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
        final String deviceName;
        final String deviceAddress;
        final String status;
        final JbdBasicInfo basicInfo;
        final JbdCellVoltages cellVoltages;
        final long updatedAtMillis;

        Snapshot(boolean connected, String deviceName, String deviceAddress, String status, JbdBasicInfo basicInfo, JbdCellVoltages cellVoltages, long updatedAtMillis) {
            this.connected = connected;
            this.deviceName = deviceName;
            this.deviceAddress = deviceAddress;
            this.status = status;
            this.basicInfo = basicInfo;
            this.cellVoltages = cellVoltages;
            this.updatedAtMillis = updatedAtMillis;
        }

        static Snapshot disconnected(String deviceName, String deviceAddress, String status) {
            return new Snapshot(false, deviceName, deviceAddress, status, null, null, System.currentTimeMillis());
        }

        Snapshot withStatus(boolean connected, String status) {
            return new Snapshot(connected, deviceName, deviceAddress, status, basicInfo, cellVoltages, System.currentTimeMillis());
        }

        Snapshot withBasicInfo(JbdBasicInfo info, String status) {
            return new Snapshot(connected, deviceName, deviceAddress, status, info, cellVoltages, System.currentTimeMillis());
        }

        Snapshot withCellVoltages(JbdCellVoltages voltages, String status) {
            return new Snapshot(connected, deviceName, deviceAddress, status, basicInfo, voltages, System.currentTimeMillis());
        }
    }
}
