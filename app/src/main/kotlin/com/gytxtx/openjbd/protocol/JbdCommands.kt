package com.gytxtx.openjbd.protocol

object JbdCommands {
    const val CMD_BASIC_INFO: Byte = 0x03
    const val CMD_CELL_VOLTAGE: Byte = 0x04
    const val CMD_FACTORY_MODE: Byte = 0x00
    const val CMD_CLOSE_FACTORY_MODE: Byte = 0x01
    const val CMD_MANUFACTURING_DATE: Byte = 0x15
    const val CMD_SERIAL_NUMBER: Byte = 0x16
    const val CMD_EXTENDED_PARAMS: Byte = 0xFA.toByte()
    const val CMD_MANUFACTURER: Byte = 0xA0.toByte()
    const val CMD_BATTERY_MODEL: Byte = 0xA1.toByte()
    const val CMD_BARCODE: Byte = 0xA2.toByte()

    @JvmStatic
    fun readBasicInfo(): ByteArray = readCommand(CMD_BASIC_INFO)

    @JvmStatic
    fun readCellVoltage(): ByteArray = readCommand(CMD_CELL_VOLTAGE)

    @JvmStatic
    fun openFactoryMode(): ByteArray = writeCommand(CMD_FACTORY_MODE, byteArrayOf(0x56, 0x78))

    @JvmStatic
    fun closeFactoryMode(): ByteArray = writeCommand(CMD_CLOSE_FACTORY_MODE, byteArrayOf(0x00, 0x00))

    @JvmStatic
    fun readExtendedParams(start: Int, length: Int): ByteArray = writeCommand(
        0xA5.toByte(), CMD_EXTENDED_PARAMS, byteArrayOf(
            ((start shr 8) and 0xFF).toByte(),
            (start and 0xFF).toByte(),
            (length and 0xFF).toByte()
        )
    )

    @JvmStatic
    fun readCommand(command: Byte): ByteArray = writeCommand(0xA5.toByte(), command, null)

    @JvmStatic
    fun writeCommand(command: Byte, payload: ByteArray?): ByteArray =
        writeCommand(0x5A.toByte(), command, payload)

    private fun writeCommand(mode: Byte, command: Byte, payload: ByteArray?): ByteArray {
        val length = payload?.size ?: 0
        val frame = ByteArray(length + 7)
        frame[0] = 0xDD.toByte()
        frame[1] = mode
        frame[2] = command
        frame[3] = length.toByte()
        if (payload != null) {
            payload.copyInto(frame, 4, 0, length)
        }
        frame[frame.size - 3] = checksumHigh(command, length, payload)
        frame[frame.size - 2] = checksumLow(command, length, payload)
        frame[frame.size - 1] = 0x77
        return frame
    }

    internal fun checksumHigh(command: Byte, length: Int, data: ByteArray?): Byte {
        val value = checksum(command, length, data)
        return ((value shr 8) and 0xFF).toByte()
    }

    internal fun checksumLow(command: Byte, length: Int, data: ByteArray?): Byte {
        val value = checksum(command, length, data)
        return (value and 0xFF).toByte()
    }

    internal fun checksum(command: Byte, length: Int, data: ByteArray?): Int {
        var sum = (command.toInt() and 0xFF) + (length and 0xFF)
        if (data != null) {
            for (b in data) {
                sum += b.toInt() and 0xFF
            }
        }
        return ((sum.inv()) + 1) and 0xFFFF
    }
}
