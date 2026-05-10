package com.gytxtx.openjbd.protocol;

import java.util.Arrays;

public final class JbdFrame {
    public final int command;
    public final int status;
    public final byte[] payload;

    private JbdFrame(int command, int status, byte[] payload) {
        this.command = command;
        this.status = status;
        this.payload = payload;
    }

    public static JbdFrame parse(byte[] frame) throws JbdParseException {
        if (frame.length < 7) {
            throw new JbdParseException("Frame too short");
        }
        if ((frame[0] & 0xFF) != 0xDD || (frame[frame.length - 1] & 0xFF) != 0x77) {
            throw new JbdParseException("Bad frame boundary");
        }
        int command = frame[1] & 0xFF;
        int status = frame[2] & 0xFF;
        int length = frame[3] & 0xFF;
        if (frame.length != length + 7) {
            throw new JbdParseException("Bad frame length");
        }
        byte[] payload = Arrays.copyOfRange(frame, 4, 4 + length);
        int expected = JbdCommands.checksum((byte) status, length, payload);
        int actual = ((frame[frame.length - 3] & 0xFF) << 8) | (frame[frame.length - 2] & 0xFF);
        if (expected != actual) {
            throw new JbdParseException("Bad checksum");
        }
        return new JbdFrame(command, status, payload);
    }
}
