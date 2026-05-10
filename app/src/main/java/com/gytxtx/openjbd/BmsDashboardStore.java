package com.gytxtx.openjbd;

import java.util.ArrayList;
import java.util.List;

final class BmsDashboardStore {
    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static Snapshot snapshot;

    private BmsDashboardStore() {
    }

    static synchronized Snapshot getSnapshot() {
        return snapshot;
    }

    static void update(Snapshot value) {
        List<Listener> listeners;
        synchronized (BmsDashboardStore.class) {
            snapshot = value;
            listeners = new ArrayList<>(LISTENERS);
        }
        for (Listener listener : listeners) {
            listener.onDashboardSnapshotChanged(value);
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
        void onDashboardSnapshotChanged(Snapshot snapshot);
    }

    static final class Snapshot {
        final int soc;
        final float voltage;
        final float current;
        final float power;

        Snapshot(int soc, float voltage, float current, float power) {
            this.soc = soc;
            this.voltage = voltage;
            this.current = current;
            this.power = power;
        }
    }
}
