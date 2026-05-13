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
    public final int balanceState;
    public final boolean[] balanceStates;
    public final int protectionState;
    public final boolean[] protectionStates;
    public final boolean chargeEnabled;
    public final boolean dischargeEnabled;
    public final int cellCount;
    public final int ntcCount;
    public final String softwareVersion;
    public final List<Float> temperaturesC;
    public final boolean hasLearnCapacity;
    public final float learnCapacityAh;
    public final boolean hasExtendedInfo;
    public final int extensionMarker;
    public final int alter;
    public final boolean hasBalanceCurrent;
    public final float balanceCurrentA;

    JbdBasicInfo(
            float totalVoltage,
            float current,
            float remainingAh,
            float nominalAh,
            int cycleCount,
            String productionDate,
            int soc,
            int balanceState,
            boolean[] balanceStates,
            int protectionState,
            boolean[] protectionStates,
            boolean chargeEnabled,
            boolean dischargeEnabled,
            int cellCount,
            int ntcCount,
            String softwareVersion,
            List<Float> temperaturesC,
            boolean hasLearnCapacity,
            float learnCapacityAh,
            boolean hasExtendedInfo,
            int extensionMarker,
            int alter,
            boolean hasBalanceCurrent,
            float balanceCurrentA
    ) {
        this.totalVoltage = totalVoltage;
        this.current = current;
        this.remainingAh = remainingAh;
        this.nominalAh = nominalAh;
        this.cycleCount = cycleCount;
        this.productionDate = productionDate;
        this.soc = soc;
        this.balanceState = balanceState;
        this.balanceStates = balanceStates.clone();
        this.protectionState = protectionState;
        this.protectionStates = protectionStates.clone();
        this.chargeEnabled = chargeEnabled;
        this.dischargeEnabled = dischargeEnabled;
        this.cellCount = cellCount;
        this.ntcCount = ntcCount;
        this.softwareVersion = softwareVersion;
        this.temperaturesC = Collections.unmodifiableList(new ArrayList<>(temperaturesC));
        this.hasLearnCapacity = hasLearnCapacity;
        this.learnCapacityAh = learnCapacityAh;
        this.hasExtendedInfo = hasExtendedInfo;
        this.extensionMarker = extensionMarker;
        this.alter = alter;
        this.hasBalanceCurrent = hasBalanceCurrent;
        this.balanceCurrentA = balanceCurrentA;
    }

    public float learnedOrNominalAh() {
        return hasLearnCapacity && learnCapacityAh > 0f ? learnCapacityAh : nominalAh;
    }

    public boolean hasActiveProtection() {
        for (boolean protectionState : protectionStates) {
            if (protectionState) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActiveBalance() {
        for (boolean balanceState : balanceStates) {
            if (balanceState) {
                return true;
            }
        }
        return false;
    }
}
