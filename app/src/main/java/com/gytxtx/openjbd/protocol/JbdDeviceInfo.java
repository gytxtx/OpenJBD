package com.gytxtx.openjbd.protocol;

public final class JbdDeviceInfo {
    public static final JbdDeviceInfo EMPTY = new JbdDeviceInfo("", "", "", "", "", "", 0f, 0f, 0f);

    public final String serialNumber;
    public final String barcode;
    public final String batteryModel;
    public final String manufacturer;
    public final String bmsModel;
    public final String bmsAddress;
    public final float ratedChargeCurrentA;
    public final float ratedDischargeCurrentA;
    public final float ratedDischargePowerW;

    private JbdDeviceInfo(
            String serialNumber,
            String barcode,
            String batteryModel,
            String manufacturer,
            String bmsModel,
            String bmsAddress,
            float ratedChargeCurrentA,
            float ratedDischargeCurrentA,
            float ratedDischargePowerW
    ) {
        this.serialNumber = clean(serialNumber);
        this.barcode = clean(barcode);
        this.batteryModel = clean(batteryModel);
        this.manufacturer = clean(manufacturer);
        this.bmsModel = clean(bmsModel);
        this.bmsAddress = clean(bmsAddress);
        this.ratedChargeCurrentA = ratedChargeCurrentA;
        this.ratedDischargeCurrentA = ratedDischargeCurrentA;
        this.ratedDischargePowerW = ratedDischargePowerW;
    }

    public JbdDeviceInfo withSerialNumber(String value) {
        return new JbdDeviceInfo(value, barcode, batteryModel, manufacturer, bmsModel, bmsAddress, ratedChargeCurrentA, ratedDischargeCurrentA, ratedDischargePowerW);
    }

    public JbdDeviceInfo withBarcode(String value) {
        return new JbdDeviceInfo(serialNumber, value, batteryModel, manufacturer, bmsModel, bmsAddress, ratedChargeCurrentA, ratedDischargeCurrentA, ratedDischargePowerW);
    }

    public JbdDeviceInfo withBatteryModel(String value) {
        return new JbdDeviceInfo(serialNumber, barcode, value, manufacturer, bmsModel, bmsAddress, ratedChargeCurrentA, ratedDischargeCurrentA, ratedDischargePowerW);
    }

    public JbdDeviceInfo withManufacturer(String value) {
        return new JbdDeviceInfo(serialNumber, barcode, batteryModel, value, bmsModel, bmsAddress, ratedChargeCurrentA, ratedDischargeCurrentA, ratedDischargePowerW);
    }

    public JbdDeviceInfo withBmsModel(String value) {
        return new JbdDeviceInfo(serialNumber, barcode, batteryModel, manufacturer, value, bmsAddress, ratedChargeCurrentA, ratedDischargeCurrentA, ratedDischargePowerW);
    }

    public JbdDeviceInfo withBmsAddress(String value) {
        return new JbdDeviceInfo(serialNumber, barcode, batteryModel, manufacturer, bmsModel, value, ratedChargeCurrentA, ratedDischargeCurrentA, ratedDischargePowerW);
    }

    public JbdDeviceInfo withRatings(float chargeCurrentA, float dischargeCurrentA, float dischargePowerW) {
        return new JbdDeviceInfo(serialNumber, barcode, batteryModel, manufacturer, bmsModel, bmsAddress, chargeCurrentA, dischargeCurrentA, dischargePowerW);
    }

    public boolean hasAnyField() {
        return serialNumber.length() > 0
                || barcode.length() > 0
                || batteryModel.length() > 0
                || manufacturer.length() > 0
                || bmsModel.length() > 0
                || bmsAddress.length() > 0
                || ratedChargeCurrentA > 0f
                || ratedDischargeCurrentA > 0f
                || ratedDischargePowerW > 0f;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u0000', ' ').trim();
    }
}
