package com.gytxtx.openjbd.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class JbdParserTest {
    @Test
    public void parseBasicInfo_decodesCoreFields() throws Exception {
        byte[] payload = new byte[32];
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
        payload[25] = 60;
        putU16(payload, 26, 258);
        putU16(payload, 28, 1900);
        putU16(payload, 30, 125);

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
        assertTrue(info.hasExtendedInfo);
        assertEquals(60, info.extensionMarker);
        assertEquals(258, info.alter);
        assertTrue(info.hasLearnCapacity);
        assertEquals(19.0f, info.learnCapacityAh, 0.001f);
        assertTrue(info.hasBalanceCurrent);
        assertEquals(1.25f, info.balanceCurrentA, 0.001f);
    }

    @Test
    public void parseBasicInfo_withoutExtensionHasNoLearnCapacity() throws Exception {
        byte[] payload = new byte[25];
        payload[22] = 1;
        putU16(payload, 23, 2981);

        JbdBasicInfo info = JbdParser.parseBasicInfo(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)));

        assertFalse(info.hasLearnCapacity);
        assertEquals(0.0f, info.learnCapacityAh, 0.001f);
        assertFalse(info.hasExtendedInfo);
        assertFalse(info.hasBalanceCurrent);
    }

    @Test
    public void parseBasicInfo_allowsExtendedInfoWithoutBalanceCurrent() throws Exception {
        byte[] payload = new byte[30];
        payload[22] = 1;
        putU16(payload, 23, 2981);
        payload[25] = 60;
        putU16(payload, 26, 258);
        putU16(payload, 28, 1900);

        JbdBasicInfo info = JbdParser.parseBasicInfo(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)));

        assertTrue(info.hasExtendedInfo);
        assertEquals(60, info.extensionMarker);
        assertEquals(258, info.alter);
        assertTrue(info.hasLearnCapacity);
        assertEquals(19.0f, info.learnCapacityAh, 0.001f);
        assertFalse(info.hasBalanceCurrent);
    }

    @Test
    public void parseBasicInfo_humidityMarkerUsesTenthsForCurrentAndRemainingCapacity() throws Exception {
        byte[] payload = new byte[32];
        putU16(payload, 2, 0xFF9C);
        putU16(payload, 4, 123);
        payload[22] = 1;
        putU16(payload, 23, 2981);
        payload[25] = (byte) 136;
        putU16(payload, 26, 1024);
        putU16(payload, 28, 1900);
        putU16(payload, 30, 250);

        JbdBasicInfo info = JbdParser.parseBasicInfo(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)));

        assertEquals(-10.0f, info.current, 0.001f);
        assertEquals(12.3f, info.remainingAh, 0.001f);
        assertEquals(136, info.extensionMarker);
        assertEquals(1024, info.alter);
        assertEquals(19.0f, info.learnCapacityAh, 0.001f);
        assertTrue(info.hasBalanceCurrent);
        assertEquals(2.5f, info.balanceCurrentA, 0.001f);
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

    @Test
    public void parseText_decodesGb2312AndTrimsPadding() throws Exception {
        byte[] payload = new byte[]{'J', 'B', 'D', 0x00, 0x20};

        String value = JbdParser.parseText(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_MANUFACTURER, 0, payload)));

        assertEquals("JBD", value);
    }

    @Test
    public void parseSerialNumber_decodesUnsignedValue() throws Exception {
        byte[] payload = new byte[2];
        putU16(payload, 0, 258);

        String value = JbdParser.parseSerialNumber(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_SERIAL_NUMBER, 0, payload)));

        assertEquals("258", value);
    }

    @Test
    public void parseExtendedParams_decodesStartAndData() throws Exception {
        byte[] payload = new byte[]{0x00, 0x75, 0x04, 0x01, 0x02, 0x03, 0x04};

        JbdParser.ExtendedParams params = JbdParser.parseExtendedParams(JbdFrame.parse(JbdFrameTest.responseFrame(JbdCommands.CMD_EXTENDED_PARAMS, 0, payload)));

        assertEquals(117, params.start);
        assertEquals(4, params.data.length);
        assertEquals(0x01, params.data[0]);
        assertEquals(0x04, params.data[3]);
    }

    @Test
    public void hexFromBytes_formatsSpaceSeparatedUppercaseBytes() {
        assertEquals("00 0A FF", JbdParser.hexFromBytes(new byte[]{0x00, 0x0A, (byte) 0xFF}));
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
