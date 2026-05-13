package com.gytxtx.openjbd.protocol;

public final class JbdCommands {
    public static final byte CMD_BASIC_INFO = 0x03;
    public static final byte CMD_CELL_VOLTAGE = 0x04;
    public static final byte CMD_FACTORY_MODE = 0x00;
    public static final byte CMD_CLOSE_FACTORY_MODE = 0x01;
    public static final byte CMD_MANUFACTURING_DATE = 0x15;
    public static final byte CMD_SERIAL_NUMBER = 0x16;
    public static final byte CMD_EXTENDED_PARAMS = (byte) 0xFA;
    public static final byte CMD_MANUFACTURER = (byte) 0xA0;
    public static final byte CMD_BATTERY_MODEL = (byte) 0xA1;
    public static final byte CMD_BARCODE = (byte) 0xA2;

    private JbdCommands() {
    }

    public static byte[] readBasicInfo() {
        return readCommand(CMD_BASIC_INFO);
    }

    public static byte[] readCellVoltage() {
        return readCommand(CMD_CELL_VOLTAGE);
    }

    public static byte[] openFactoryMode() {
        return writeCommand(CMD_FACTORY_MODE, new byte[]{0x56, 0x78});
    }

    public static byte[] closeFactoryMode() {
        return writeCommand(CMD_CLOSE_FACTORY_MODE, new byte[]{0x00, 0x00});
    }

    public static byte[] readExtendedParams(int start, int length) {
        return writeCommand((byte) 0xA5, CMD_EXTENDED_PARAMS, new byte[]{
                (byte) ((start >> 8) & 0xFF),
                (byte) (start & 0xFF),
                (byte) (length & 0xFF)
        });
    }

    public static byte[] readCommand(byte command) {
        return writeCommand((byte) 0xA5, command, null);
    }

    public static byte[] writeCommand(byte command, byte[] payload) {
        return writeCommand((byte) 0x5A, command, payload);
    }

    private static byte[] writeCommand(byte mode, byte command, byte[] payload) {
        int length = payload == null ? 0 : payload.length;
        byte[] frame = new byte[length + 7];
        frame[0] = (byte) 0xDD;
        frame[1] = mode;
        frame[2] = command;
        frame[3] = (byte) length;
        if (payload != null) {
            System.arraycopy(payload, 0, frame, 4, length);
        }
        frame[frame.length - 3] = checksumHigh(command, length, payload);
        frame[frame.length - 2] = checksumLow(command, length, payload);
        frame[frame.length - 1] = 0x77;
        return frame;
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
