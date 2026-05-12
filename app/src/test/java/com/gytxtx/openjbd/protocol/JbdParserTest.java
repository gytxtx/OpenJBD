package com.gytxtx.openjbd.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class JbdParserTest {
    @Test
    public void parseBasicInfo_decodesCoreFields() throws Exception {
        byte[] payload = new byte[25];
        putU16(payload, 0, 5234);
        putU16(payload, 2, 0xFB2E);
        putU16(payload, 4, 1000);
        putU16(payload, 6, 2000);
        putU16(payload, 8, 42);
        putU16(payload, 10, (24 << 9) | (5 << 5) | 12);
        payload[18] = 0x21;
        payload[19] = 88;
        payload[20] = 0x03;
        payload[21] = 4;
        payload[22] = 1;
        putU16(payload, 23, 2981);

        JbdBasicInfo info = JbdParser.parseBasicInfo(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)));

        assertEquals(52.34f, info.totalVoltage, 0.001f);
        assertEquals(-12.34f, info.current, 0.001f);
        assertEquals(10.0f, info.remainingAh, 0.001f);
        assertEquals(20.0f, info.nominalAh, 0.001f);
        assertEquals(42, info.cycleCount);
        assertEquals("2024-5-12", info.productionDate);
        assertEquals("2.1", info.softwareVersion);
        assertEquals(88, info.soc);
        assertTrue(info.chargeEnabled);
        assertTrue(info.dischargeEnabled);
        assertEquals(4, info.cellCount);
        assertEquals(1, info.ntcCount);
        assertEquals(25.0f, info.temperaturesC.get(0), 0.001f);
    }

    @Test
    public void parseCellVoltages_decodesSummary() throws Exception {
        byte[] payload = new byte[6];
        putU16(payload, 0, 3300);
        putU16(payload, 2, 3310);
        putU16(payload, 4, 3290);

        JbdCellVoltages voltages = JbdParser.parseCellVoltages(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_CELL_VOLTAGE, 0, payload)));

        assertEquals(3, voltages.cells.size());
        assertEquals(3.29f, voltages.min, 0.001f);
        assertEquals(3.31f, voltages.max, 0.001f);
        assertEquals(0.02f, voltages.delta, 0.001f);
        assertEquals(3.30f, voltages.average, 0.001f);
    }

    @Test(expected = JbdParseException.class)
    public void parseBasicInfo_rejectsBmsErrorStatus() throws Exception {
        JbdParser.parseBasicInfo(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 1, new byte[23])));
    }

    @Test
    public void parseBasicInfo_handlesFetBitsIndependently() throws Exception {
        byte[] payload = new byte[23];
        payload[20] = 0x01;

        JbdBasicInfo info = JbdParser.parseBasicInfo(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)));

        assertTrue(info.chargeEnabled);
        assertFalse(info.dischargeEnabled);
    }

    private static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }
}
