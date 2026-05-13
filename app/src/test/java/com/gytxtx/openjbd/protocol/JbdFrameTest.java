package com.gytxtx.openjbd.protocol;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class JbdFrameTest {
    @Test
    public void parse_validResponseFrame() throws Exception {
        byte[] payload = new byte[]{0x12, 0x34};
        JbdFrame frame = JbdFrame.parse(responseFrame(JbdCommands.CMD_BASIC_INFO, 0, payload));

        assertEquals(JbdCommands.CMD_BASIC_INFO & 0xFF, frame.command);
        assertEquals(0, frame.status);
        assertArrayEquals(payload, frame.payload);
    }

    @Test(expected = JbdParseException.class)
    public void parse_rejectsBadChecksum() throws Exception {
        byte[] frame = responseFrame(JbdCommands.CMD_BASIC_INFO, 0, new byte[]{0x12, 0x34});
        frame[frame.length - 2] ^= 0x01;

        JbdFrame.parse(frame);
    }

    @Test(expected = JbdParseException.class)
    public void parse_rejectsBadLength() throws Exception {
        byte[] frame = responseFrame(JbdCommands.CMD_BASIC_INFO, 0, new byte[]{0x12, 0x34});
        frame[3] = 0x03;

        JbdFrame.parse(frame);
    }

    @Test
    public void commands_buildFactoryModeFrames() {
        assertArrayEquals(
                new byte[]{(byte) 0xDD, 0x5A, 0x00, 0x02, 0x56, 0x78, (byte) 0xFF, 0x30, 0x77},
                JbdCommands.openFactoryMode());
        assertArrayEquals(
                new byte[]{(byte) 0xDD, 0x5A, 0x01, 0x02, 0x00, 0x00, (byte) 0xFF, (byte) 0xFD, 0x77},
                JbdCommands.closeFactoryMode());
    }

    @Test
    public void commands_buildExtendedParamReadFrame() {
        assertArrayEquals(
                new byte[]{(byte) 0xDD, (byte) 0xA5, (byte) 0xFA, 0x03, 0x00, 0x75, 0x04, (byte) 0xFE, (byte) 0x8A, 0x77},
                JbdCommands.readExtendedParams(117, 4));
    }

    static byte[] responseFrame(byte command, int status, byte[] payload) {
        int length = payload.length;
        byte[] frame = new byte[length + 7];
        frame[0] = (byte) 0xDD;
        frame[1] = command;
        frame[2] = (byte) status;
        frame[3] = (byte) length;
        System.arraycopy(payload, 0, frame, 4, length);
        int checksum = JbdCommands.checksum((byte) status, length, payload);
        frame[frame.length - 3] = (byte) ((checksum >> 8) & 0xFF);
        frame[frame.length - 2] = (byte) (checksum & 0xFF);
        frame[frame.length - 1] = 0x77;
        return frame;
    }
}
