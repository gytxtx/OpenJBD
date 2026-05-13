package com.gytxtx.openjbd.protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class JbdParser {
    private static final Charset GB2312 = Charset.forName("GB2312");

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
        boolean hasExtendedInfo = false;
        int extensionMarker = 0;
        int alter = 0;
        boolean hasBalanceCurrent = false;
        float balanceCurrentA = 0f;
        int extraStart = 23 + ntcCount * 2;
        if (extraStart < p.length) {
            extensionMarker = p[extraStart] & 0xFF;
        }
        if (extraStart + 2 < p.length) {
            alter = u16(p, extraStart + 1);
        }
        if (extraStart + 4 < p.length) {
            hasExtendedInfo = true;
            learnCapacityAh = u16(p, extraStart + 3) / 100f;
            hasLearnCapacity = learnCapacityAh > 0f;
            if (extensionMarker == 136) {
                current = s16(p, 2) / 10f;
                remainingAh = u16(p, 4) / 10f;
            }
        }
        if (extraStart + 6 < p.length) {
            hasBalanceCurrent = true;
            balanceCurrentA = u16(p, extraStart + 5) / 100f;
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
                learnCapacityAh,
                hasExtendedInfo,
                extensionMarker,
                alter,
                hasBalanceCurrent,
                balanceCurrentA
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

    public static String parseText(JbdFrame frame) throws JbdParseException {
        if (frame.status != 0) {
            throw new JbdParseException("BMS returned status " + frame.status);
        }
        return new String(frame.payload, GB2312).replace('\u0000', ' ').trim();
    }

    public static String parseSerialNumber(JbdFrame frame) throws JbdParseException {
        if (frame.status != 0) {
            throw new JbdParseException("BMS returned status " + frame.status);
        }
        if (frame.payload.length < 2) {
            throw new JbdParseException("Serial-number payload too short");
        }
        return Integer.toString(u16(frame.payload, 0));
    }

    public static ExtendedParams parseExtendedParams(JbdFrame frame) throws JbdParseException {
        if (frame.command != (JbdCommands.CMD_EXTENDED_PARAMS & 0xFF)) {
            throw new JbdParseException("Not an extended-params frame");
        }
        if (frame.status != 0) {
            throw new JbdParseException("BMS returned status " + frame.status);
        }
        if (frame.payload.length < 3) {
            throw new JbdParseException("Extended-params payload too short");
        }
        return new ExtendedParams(u16(frame.payload, 0), copyOfRange(frame.payload, 3, frame.payload.length));
    }

    static int u16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    static int s16(byte[] data, int offset) {
        int value = u16(data, offset);
        return value >= 0x8000 ? value - 0x10000 : value;
    }

    public static String textFromBytes(byte[] data) {
        return new String(data, GB2312).replace('\u0000', ' ').trim();
    }

    public static String hexFromBytes(byte[] data) {
        char[] table = "0123456789ABCDEF".toCharArray();
        StringBuilder builder = new StringBuilder(data.length * 3);
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            int value = data[i] & 0xFF;
            builder.append(table[value >>> 4]);
            builder.append(table[value & 0x0F]);
        }
        return builder.toString();
    }

    private static byte[] copyOfRange(byte[] data, int start, int end) {
        byte[] out = new byte[end - start];
        System.arraycopy(data, start, out, 0, out.length);
        return out;
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

    public static final class ExtendedParams {
        public final int start;
        public final byte[] data;

        ExtendedParams(int start, byte[] data) {
            this.start = start;
            this.data = data;
        }
    }
}
