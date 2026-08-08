package com.gytxtx.openjbd.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JbdFrameAssemblerTest {
    @Test
    fun append_reassemblesSplitFrame() {
        val assembler = JbdFrameAssembler()
        val frame = JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, byteArrayOf(0x01, 0x02))

        assertTrue(assembler.append(frame.copyOfRange(0, 4)).isEmpty())
        val frames = assembler.append(frame.copyOfRange(4, frame.size))

        assertEquals(1, frames.size)
        assertEquals(JbdCommands.CMD_BASIC_INFO.toInt() and 0xFF, frames[0].command)
    }

    @Test
    fun append_extractsMultipleFramesAndSkipsNoise() {
        val assembler = JbdFrameAssembler()
        val first = JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, byteArrayOf(0x01))
        val second = JbdFrameTest.responseFrame(JbdCommands.CMD_CELL_VOLTAGE, 0, byteArrayOf(0x0C, 0xE4.toByte()))
        val data = byteArrayOf(0x55, 0x66) + first + second

        val frames = assembler.append(data)

        assertEquals(2, frames.size)
        assertEquals(JbdCommands.CMD_BASIC_INFO.toInt() and 0xFF, frames[0].command)
        assertEquals(JbdCommands.CMD_CELL_VOLTAGE.toInt() and 0xFF, frames[1].command)
    }
}
