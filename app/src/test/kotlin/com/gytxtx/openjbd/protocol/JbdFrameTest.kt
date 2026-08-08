package com.gytxtx.openjbd.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class JbdFrameTest {
    @Test
    fun parse_validResponseFrame() {
        val payload = byteArrayOf(0x12, 0x34)
        val frame = JbdFrame.parse(responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload))

        assertEquals(JbdCommands.CMD_BASIC_INFO.toInt() and 0xFF, frame.command)
        assertEquals(0, frame.status)
        assertArrayEquals(payload, frame.payload)
    }

    @Test(expected = JbdParseException::class)
    fun parse_rejectsBadChecksum() {
        val frame = responseFrame(JbdCommands.CMD_BASIC_INFO, 0, byteArrayOf(0x12, 0x34))
        frame[frame.size - 2] = (frame[frame.size - 2].toInt() xor 0x01).toByte()

        JbdFrame.parse(frame)
    }

    @Test(expected = JbdParseException::class)
    fun parse_rejectsBadLength() {
        val frame = responseFrame(JbdCommands.CMD_BASIC_INFO, 0, byteArrayOf(0x12, 0x34))
        frame[3] = 0x03

        JbdFrame.parse(frame)
    }

    @Test
    fun commands_buildFactoryModeFrames() {
        assertArrayEquals(
            byteArrayOf(0xDD.toByte(), 0x5A, 0x00, 0x02, 0x56, 0x78, 0xFF.toByte(), 0x30, 0x77),
            JbdCommands.openFactoryMode()
        )
        assertArrayEquals(
            byteArrayOf(0xDD.toByte(), 0x5A, 0x01, 0x02, 0x00, 0x00, 0xFF.toByte(), 0xFD.toByte(), 0x77),
            JbdCommands.closeFactoryMode()
        )
    }

    @Test
    fun commands_buildExtendedParamReadFrame() {
        assertArrayEquals(
            byteArrayOf(0xDD.toByte(), 0xA5.toByte(), 0xFA.toByte(), 0x03, 0x00, 0x75, 0x04, 0xFE.toByte(), 0x8A.toByte(), 0x77),
            JbdCommands.readExtendedParams(117, 4)
        )
    }

    companion object {
        internal fun responseFrame(command: Byte, status: Int, payload: ByteArray): ByteArray {
            val length = payload.size
            val frame = ByteArray(length + 7)
            frame[0] = 0xDD.toByte()
            frame[1] = command
            frame[2] = status.toByte()
            frame[3] = length.toByte()
            payload.copyInto(frame, 4, 0, length)
            val checksum = JbdCommands.checksum(status.toByte(), length, payload)
            frame[frame.size - 3] = ((checksum shr 8) and 0xFF).toByte()
            frame[frame.size - 2] = (checksum and 0xFF).toByte()
            frame[frame.size - 1] = 0x77
            return frame
        }
    }
}
