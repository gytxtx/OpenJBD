package com.gytxtx.openjbd.protocol

class JbdDeviceInfo(
    @JvmField val serialNumber: String,
    @JvmField val barcode: String,
    @JvmField val batteryModel: String,
    @JvmField val manufacturer: String,
    @JvmField val bmsModel: String,
    @JvmField val bmsAddress: String,
    @JvmField val ratedChargeCurrentA: Float,
    @JvmField val ratedDischargeCurrentA: Float,
    @JvmField val ratedDischargePowerW: Float
) {
    companion object {
        @JvmField
        val EMPTY: JbdDeviceInfo = JbdDeviceInfo("", "", "", "", "", "", 0f, 0f, 0f)
    }

    fun withSerialNumber(value: String) = copy(serialNumber = value)

    fun withBarcode(value: String) = copy(barcode = value)

    fun withBatteryModel(value: String) = copy(batteryModel = value)

    fun withManufacturer(value: String) = copy(manufacturer = value)

    fun withBmsModel(value: String) = copy(bmsModel = value)

    fun withBmsAddress(value: String) = copy(bmsAddress = value)

    fun withRatings(chargeCurrentA: Float, dischargeCurrentA: Float, dischargePowerW: Float) =
        copy(
            ratedChargeCurrentA = chargeCurrentA,
            ratedDischargeCurrentA = dischargeCurrentA,
            ratedDischargePowerW = dischargePowerW
        )

    fun copy(
        serialNumber: String = this.serialNumber,
        barcode: String = this.barcode,
        batteryModel: String = this.batteryModel,
        manufacturer: String = this.manufacturer,
        bmsModel: String = this.bmsModel,
        bmsAddress: String = this.bmsAddress,
        ratedChargeCurrentA: Float = this.ratedChargeCurrentA,
        ratedDischargeCurrentA: Float = this.ratedDischargeCurrentA,
        ratedDischargePowerW: Float = this.ratedDischargePowerW
    ) = JbdDeviceInfo(
        serialNumber, barcode, batteryModel, manufacturer, bmsModel, bmsAddress,
        ratedChargeCurrentA, ratedDischargeCurrentA, ratedDischargePowerW
    )

    fun hasAnyField(): Boolean =
        serialNumber.isNotEmpty()
                || barcode.isNotEmpty()
                || batteryModel.isNotEmpty()
                || manufacturer.isNotEmpty()
                || bmsModel.isNotEmpty()
                || bmsAddress.isNotEmpty()
                || ratedChargeCurrentA > 0f
                || ratedDischargeCurrentA > 0f
                || ratedDischargePowerW > 0f
}
