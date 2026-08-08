package com.gytxtx.openjbd.protocol

import java.io.ByteArrayOutputStream

class JbdFrameAssembler {
    private val buffer = ByteArrayOutputStream()

    fun reset() {
        buffer.reset()
    }

    fun append(chunk: ByteArray): List<JbdFrame> {
        buffer.write(chunk, 0, chunk.size)
        val frames = mutableListOf<JbdFrame>()
        val data = buffer.toByteArray()
        var cursor = 0
        while (cursor < data.size) {
            while (cursor < data.size && (data[cursor].toInt() and 0xFF) != 0xDD) {
                cursor++
            }
            if (data.size - cursor < 4) {
                break
            }
            val length = data[cursor + 3].toInt() and 0xFF
            val frameLength = length + 7
            if (data.size - cursor < frameLength) {
                break
            }
            val rawFrame = data.copyOfRange(cursor, cursor + frameLength)
            try {
                frames.add(JbdFrame.parse(rawFrame))
            } catch (_: JbdParseException) {
                // Drop malformed bytes and continue looking for the next frame.
            }
            cursor += frameLength
        }
        buffer.reset()
        if (cursor < data.size) {
            buffer.write(data, cursor, data.size - cursor)
        }
        return frames
    }
}
