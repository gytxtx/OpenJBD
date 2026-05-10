package com.gytxtx.openjbd.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JbdBasicInfo {
    public final float totalVoltage;
    public final float current;
    public final float remainingAh;
    public final float nominalAh;
    public final int cycleCount;
    public final String productionDate;
    public final int soc;
    public final boolean chargeEnabled;
    public final boolean dischargeEnabled;
    public final int cellCount;
    public final int ntcCount;
    public final String softwareVersion;
    public final List<Float> temperaturesC;

    JbdBasicInfo(
            float totalVoltage,
            float current,
            float remainingAh,
            float nominalAh,
            int cycleCount,
            String productionDate,
            int soc,
            boolean chargeEnabled,
            boolean dischargeEnabled,
            int cellCount,
            int ntcCount,
            String softwareVersion,
            List<Float> temperaturesC
    ) {
        this.totalVoltage = totalVoltage;
        this.current = current;
        this.remainingAh = remainingAh;
        this.nominalAh = nominalAh;
        this.cycleCount = cycleCount;
        this.productionDate = productionDate;
        this.soc = soc;
        this.chargeEnabled = chargeEnabled;
        this.dischargeEnabled = dischargeEnabled;
        this.cellCount = cellCount;
        this.ntcCount = ntcCount;
        this.softwareVersion = softwareVersion;
        this.temperaturesC = Collections.unmodifiableList(new ArrayList<>(temperaturesC));
    }
}
