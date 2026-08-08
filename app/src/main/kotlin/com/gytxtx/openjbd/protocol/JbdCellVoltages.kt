package com.gytxtx.openjbd.protocol

class JbdCellVoltages(cells: List<Float>) {
    @JvmField val cells: List<Float> = cells.toList()
    @JvmField val min: Float
    @JvmField val max: Float
    @JvmField val delta: Float
    @JvmField val average: Float

    init {
        var localMin = 0f
        var localMax = 0f
        var sum = 0f
        for (cell in cells) {
            if (localMin == 0f || cell < localMin) {
                localMin = cell
            }
            if (cell > localMax) {
                localMax = cell
            }
            sum += cell
        }
        min = localMin
        max = localMax
        delta = localMax - localMin
        average = if (cells.isEmpty()) 0f else sum / cells.size
    }
}
