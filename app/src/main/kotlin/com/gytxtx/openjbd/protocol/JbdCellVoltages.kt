package com.gytxtx.openjbd.protocol

class JbdCellVoltages(cells: List<Float>) {
    @JvmField val cells: List<Float> = cells.toList()
    @JvmField val min: Float
    @JvmField val max: Float
    @JvmField val delta: Float
    @JvmField val average: Float

    init {
        min = cells.minOrNull() ?: 0f
        max = cells.maxOrNull() ?: 0f
        delta = max - min
        average = if (cells.isEmpty()) 0f else cells.average().toFloat()
    }
}
