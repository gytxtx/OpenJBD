package com.gytxtx.openjbd.protocol;

public final class JbdCommands {
    public static final byte CMD_BASIC_INFO = 0x03;
    public static final byte CMD_CELL_VOLTAGE = 0x04;

    private JbdCommands() {
    }

    public static byte[] readBasicInfo() {
        return readCommand(CMD_BASIC_INFO);
    }

    public static byte[] readCellVoltage() {
        return readCommand(CMD_CELL_VOLTAGE);
    }

    public static byte[] readCommand(byte command) {
        return new byte[]{
                (byte) 0xDD,
                (byte) 0xA5,
                command,
                0x00,
                checksumHigh(command, 0, null),
                checksumLow(command, 0, null),
                0x77
        };
    }

    static byte checksumHigh(byte command, int length, byte[] data) {
        int value = checksum(command, length, data);
        return (byte) ((value >> 8) & 0xFF);
    }

    static byte checksumLow(byte command, int length, byte[] data) {
        int value = checksum(command, length, data);
        return (byte) (value & 0xFF);
    }

    static int checksum(byte command, int length, byte[] data) {
        int sum = (command & 0xFF) + (length & 0xFF);
        if (data != null) {
            for (byte b : data) {
                sum += b & 0xFF;
            }
        }
        return ((~sum) + 1) & 0xFFFF;
    }
}
