package com.gytxtx.openjbd

object BmsDashboardStore {
    @Volatile
    private var _snapshot: Snapshot? = null
    private val listeners = mutableListOf<Listener>()

    @JvmStatic
    @Synchronized
    fun getSnapshot(): Snapshot? = _snapshot

    @JvmStatic
    fun update(value: Snapshot?) {
        val copy: List<Listener>
        synchronized(this) {
            _snapshot = value
            copy = listeners.toList()
        }
        for (listener in copy) {
            listener.onDashboardSnapshotChanged(value)
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
        fun onDashboardSnapshotChanged(snapshot: Snapshot?)
    }

    class Snapshot(
        @JvmField val soc: Int,
        @JvmField val voltage: Float,
        @JvmField val current: Float,
        @JvmField val power: Float
    )
}
