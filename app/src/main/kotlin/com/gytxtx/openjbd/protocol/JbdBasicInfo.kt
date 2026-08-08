package com.gytxtx.openjbd.protocol

class JbdBasicInfo(
    @JvmField val totalVoltage: Float,
    @JvmField val current: Float,
    @JvmField val remainingAh: Float,
    @JvmField val nominalAh: Float,
    @JvmField val cycleCount: Int,
    @JvmField val productionDate: String,
    @JvmField val soc: Int,
    @JvmField val balanceState: Int,
    @JvmField val balanceStates: BooleanArray,
    @JvmField val protectionState: Int,
    @JvmField val protectionStates: BooleanArray,
    @JvmField val chargeEnabled: Boolean,
    @JvmField val dischargeEnabled: Boolean,
    @JvmField val cellCount: Int,
    @JvmField val ntcCount: Int,
    @JvmField val softwareVersion: String,
    @JvmField val temperaturesC: List<Float>,
    @JvmField val hasLearnCapacity: Boolean,
    @JvmField val learnCapacityAh: Float,
    @JvmField val hasExtendedInfo: Boolean,
    @JvmField val extensionMarker: Int,
    @JvmField val alter: Int,
    @JvmField val hasBalanceCurrent: Boolean,
    @JvmField val balanceCurrentA: Float
) {
    val learnedOrNominalAh: Float
        get() = if (hasLearnCapacity && learnCapacityAh > 0f) learnCapacityAh else nominalAh
}
