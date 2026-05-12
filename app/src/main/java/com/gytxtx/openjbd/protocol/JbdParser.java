package com.gytxtx.openjbd.protocol;

import java.util.ArrayList;
import java.util.List;

public final class JbdParser {
    private JbdParser() {
    }

    public static JbdBasicInfo parseBasicInfo(JbdFrame frame) throws JbdParseException {
        if (frame.command != (JbdCommands.CMD_BASIC_INFO & 0xFF)) {
            throw new JbdParseException("Not a basic-info frame");
        }
        if (frame.status != 0) {
            throw new JbdParseException("BMS returned status " + frame.status);
        }
        byte[] p = frame.payload;
        if (p.length < 23) {
            throw new JbdParseException("Basic-info payload too short");
        }

        float totalVoltage = u16(p, 0) / 100f;
        float current = s16(p, 2) / 100f;
        float remainingAh = u16(p, 4) / 100f;
        float nominalAh = u16(p, 6) / 100f;
        int cycleCount = u16(p, 8);
        String productionDate = productionDate(u16(p, 10));
        String version = softwareVersion(p[18] & 0xFF);
        int soc = p[19] & 0xFF;
        int fetState = p[20] & 0xFF;
        int cellCount = p[21] & 0xFF;
        int ntcCount = p[22] & 0xFF;

        List<Float> temps = new ArrayList<>();
        for (int i = 0; i < ntcCount; i++) {
            int offset = 23 + i * 2;
            if (offset + 1 >= p.length) {
                break;
            }
            temps.add((u16(p, offset) - 2731) / 10f);
        }

        boolean hasLearnCapacity = false;
        float learnCapacityAh = 0f;
        int extraStart = 23 + ntcCount * 2;
        if (extraStart + 4 < p.length) {
            int humidity = p[extraStart] & 0xFF;
            learnCapacityAh = u16(p, extraStart + 3) / 100f;
            hasLearnCapacity = learnCapacityAh > 0f;
            if (humidity == 136) {
                current = s16(p, 2) / 10f;
                remainingAh = u16(p, 4) / 10f;
            }
        }

        return new JbdBasicInfo(
                totalVoltage,
                current,
                remainingAh,
                nominalAh,
                cycleCount,
                productionDate,
                soc,
                (fetState & 0x01) != 0,
                (fetState & 0x02) != 0,
                cellCount,
                ntcCount,
                version,
                temps,
                hasLearnCapacity,
                learnCapacityAh
        );
    }

    public static JbdCellVoltages parseCellVoltages(JbdFrame frame) throws JbdParseException {
        if (frame.command != (JbdCommands.CMD_CELL_VOLTAGE & 0xFF)) {
            throw new JbdParseException("Not a cell-voltage frame");
        }
        if (frame.status != 0) {
            throw new JbdParseException("BMS returned status " + frame.status);
        }
        if ((frame.payload.length % 2) != 0) {
            throw new JbdParseException("Odd cell-voltage payload length");
        }
        List<Float> cells = new ArrayList<>();
        for (int i = 0; i < frame.payload.length; i += 2) {
            cells.add(u16(frame.payload, i) / 1000f);
        }
        return new JbdCellVoltages(cells);
    }

    static int u16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    static int s16(byte[] data, int offset) {
        int value = u16(data, offset);
        return value >= 0x8000 ? value - 0x10000 : value;
    }

    private static String productionDate(int value) {
        int year = 2000 + (value >> 9);
        int month = (value >> 5) & 0x0F;
        int day = value & 0x1F;
        return year + "-" + month + "-" + day;
    }

    private static String softwareVersion(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        if (hex.length() == 1) {
            return hex;
        }
        return hex.substring(0, 1) + "." + hex.substring(1);
    }
}
