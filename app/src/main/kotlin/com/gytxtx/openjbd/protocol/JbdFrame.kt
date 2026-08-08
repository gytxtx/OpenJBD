package com.gytxtx.openjbd.protocol

class JbdFrame
private constructor(
    @JvmField val command: Int,
    @JvmField val status: Int,
    @JvmField val payload: ByteArray
) {
    companion object {
        @JvmStatic
        fun parse(frame: ByteArray): JbdFrame {
            if (frame.size < 7) {
                throw JbdParseException("Frame too short")
            }
            if ((frame[0].toInt() and 0xFF) != 0xDD || (frame[frame.size - 1].toInt() and 0xFF) != 0x77) {
                throw JbdParseException("Bad frame boundary")
            }
            val command = frame[1].toInt() and 0xFF
            val status = frame[2].toInt() and 0xFF
            val length = frame[3].toInt() and 0xFF
            if (frame.size != length + 7) {
                throw JbdParseException("Bad frame length")
            }
            val payload = frame.copyOfRange(4, 4 + length)
            val expected = JbdCommands.checksum(status.toByte(), length, payload)
            val actual =
                ((frame[frame.size - 3].toInt() and 0xFF) shl 8) or (frame[frame.size - 2].toInt() and 0xFF)
            if (expected != actual) {
                throw JbdParseException("Bad checksum")
            }
            return JbdFrame(command, status, payload)
        }
    }
}
