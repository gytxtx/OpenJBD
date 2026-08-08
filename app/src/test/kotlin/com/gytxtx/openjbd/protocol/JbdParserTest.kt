package com.gytxtx.openjbd.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JbdParserTest {
    @Test
    fun parseBasicInfo_decodesCoreFields() {
        val payload = ByteArray(32)
        putU16(payload, 0, 5234)
        putU16(payload, 2, 0xFB2E)
        putU16(payload, 4, 1000)
        putU16(payload, 6, 2000)
        putU16(payload, 8, 42)
        putU16(payload, 10, (24 shl 9) or (5 shl 5) or 12)
        payload[13] = 0x01
        payload[16] = 0x01
        payload[17] = 0x02
        payload[18] = 0x21
        payload[19] = 88
        payload[20] = 0x03
        payload[21] = 4
        payload[22] = 1
        putU16(payload, 23, 2981)
        payload[25] = 60
        putU16(payload, 26, 258)
        putU16(payload, 28, 1900)
        putU16(payload, 30, 125)

        val info = JbdParser.parseBasicInfo(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)
            )
        )

        assertEquals(52.34f, info.totalVoltage, 0.001f)
        assertEquals(-12.34f, info.current, 0.001f)
        assertEquals(10.0f, info.remainingAh, 0.001f)
        assertEquals(20.0f, info.nominalAh, 0.001f)
        assertEquals(42, info.cycleCount)
        assertEquals("2024-5-12", info.productionDate)
        assertTrue(info.balanceStates[0])
        assertFalse(info.balanceStates[1])
        assertEquals(0x00010000, info.balanceState)
        assertEquals(0x0102, info.protectionState)
        assertTrue(info.protectionStates[1])
        assertTrue(info.protectionStates[8])
        assertFalse(info.protectionStates[0])
        assertEquals("2.1", info.softwareVersion)
        assertEquals(88, info.soc)
        assertTrue(info.chargeEnabled)
        assertTrue(info.dischargeEnabled)
        assertEquals(4, info.cellCount)
        assertEquals(1, info.ntcCount)
        assertEquals(25.0f, info.temperaturesC[0], 0.001f)
        assertTrue(info.hasExtendedInfo)
        assertEquals(60, info.extensionMarker)
        assertEquals(258, info.alter)
        assertTrue(info.hasLearnCapacity)
        assertEquals(19.0f, info.learnCapacityAh, 0.001f)
        assertTrue(info.hasBalanceCurrent)
        assertEquals(1.25f, info.balanceCurrentA, 0.001f)
    }

    @Test
    fun parseBasicInfo_withoutExtensionHasNoLearnCapacity() {
        val payload = ByteArray(25)
        payload[22] = 1
        putU16(payload, 23, 2981)

        val info = JbdParser.parseBasicInfo(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)
            )
        )

        assertFalse(info.hasLearnCapacity)
        assertEquals(0.0f, info.learnCapacityAh, 0.001f)
        assertFalse(info.hasExtendedInfo)
        assertFalse(info.hasBalanceCurrent)
    }

    @Test
    fun parseBasicInfo_allowsExtendedInfoWithoutBalanceCurrent() {
        val payload = ByteArray(30)
        payload[22] = 1
        putU16(payload, 23, 2981)
        payload[25] = 60
        putU16(payload, 26, 258)
        putU16(payload, 28, 1900)

        val info = JbdParser.parseBasicInfo(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)
            )
        )

        assertTrue(info.hasExtendedInfo)
        assertEquals(60, info.extensionMarker)
        assertEquals(258, info.alter)
        assertTrue(info.hasLearnCapacity)
        assertEquals(19.0f, info.learnCapacityAh, 0.001f)
        assertFalse(info.hasBalanceCurrent)
    }

    @Test
    fun parseBasicInfo_humidityMarkerUsesTenthsForCurrentAndRemainingCapacity() {
        val payload = ByteArray(32)
        putU16(payload, 2, 0xFF9C)
        putU16(payload, 4, 123)
        payload[22] = 1
        putU16(payload, 23, 2981)
        payload[25] = 136.toByte()
        putU16(payload, 26, 1024)
        putU16(payload, 28, 1900)
        putU16(payload, 30, 250)

        val info = JbdParser.parseBasicInfo(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)
            )
        )

        assertEquals(-10.0f, info.current, 0.001f)
        assertEquals(12.3f, info.remainingAh, 0.001f)
        assertEquals(136, info.extensionMarker)
        assertEquals(1024, info.alter)
        assertEquals(19.0f, info.learnCapacityAh, 0.001f)
        assertTrue(info.hasBalanceCurrent)
        assertEquals(2.5f, info.balanceCurrentA, 0.001f)
    }

    @Test
    fun parseCellVoltages_decodesSummary() {
        val payload = ByteArray(6)
        putU16(payload, 0, 3300)
        putU16(payload, 2, 3310)
        putU16(payload, 4, 3290)

        val voltages = JbdParser.parseCellVoltages(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_CELL_VOLTAGE, 0, payload)
            )
        )

        assertEquals(3, voltages.cells.size)
        assertEquals(3.29f, voltages.min, 0.001f)
        assertEquals(3.31f, voltages.max, 0.001f)
        assertEquals(0.02f, voltages.delta, 0.001f)
        assertEquals(3.30f, voltages.average, 0.001f)
    }

    @Test
    fun parseText_decodesGb2312AndTrimsPadding() {
        val payload = byteArrayOf('J'.code.toByte(), 'B'.code.toByte(), 'D'.code.toByte(), 0x00, 0x20)

        val value = JbdParser.parseText(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_MANUFACTURER, 0, payload)
            )
        )

        assertEquals("JBD", value)
    }

    @Test
    fun parseSerialNumber_decodesUnsignedValue() {
        val payload = ByteArray(2)
        putU16(payload, 0, 258)

        val value = JbdParser.parseSerialNumber(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_SERIAL_NUMBER, 0, payload)
            )
        )

        assertEquals("258", value)
    }

    @Test
    fun parseExtendedParams_decodesStartAndData() {
        val payload = byteArrayOf(0x00, 0x75, 0x04, 0x01, 0x02, 0x03, 0x04)

        val params = JbdParser.parseExtendedParams(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_EXTENDED_PARAMS, 0, payload)
            )
        )

        assertEquals(117, params.start)
        assertEquals(4, params.data.size)
        assertEquals(0x01.toByte(), params.data[0])
        assertEquals(0x04.toByte(), params.data[3])
    }

    @Test
    fun parseExtendedParams_decodesRatingsPayload() {
        val payload = byteArrayOf(0x00, 0x75, 0x08, 0x00, 0x00, 0x00, 0x64, 0x00, 0x32, 0x03, 0xE8.toByte())

        val params = JbdParser.parseExtendedParams(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_EXTENDED_PARAMS, 0, payload)
            )
        )

        assertEquals(117, params.start)
        assertEquals(8, params.data.size)
        assertEquals(0x00.toByte(), params.data[0])
        assertEquals(0xE8.toByte(), params.data[7])
    }

    @Test
    fun hexFromBytes_formatsSpaceSeparatedUppercaseBytes() {
        assertEquals("00 0A FF", JbdParser.hexFromBytes(byteArrayOf(0x00, 0x0A, 0xFF.toByte())))
    }

    @Test(expected = JbdParseException::class)
    fun parseBasicInfo_rejectsBmsErrorStatus() {
        JbdParser.parseBasicInfo(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 1, ByteArray(23))
            )
        )
    }

    @Test
    fun parseBasicInfo_handlesFetBitsIndependently() {
        val payload = ByteArray(23)
        payload[20] = 0x01

        val info = JbdParser.parseBasicInfo(
            JbdFrame.parse(
                JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload)
            )
        )

        assertTrue(info.chargeEnabled)
        assertFalse(info.dischargeEnabled)
    }

    companion object {
        private fun putU16(data: ByteArray, offset: Int, value: Int) {
            data[offset] = ((value shr 8) and 0xFF).toByte()
            data[offset + 1] = (value and 0xFF).toByte()
        }
    }
}
