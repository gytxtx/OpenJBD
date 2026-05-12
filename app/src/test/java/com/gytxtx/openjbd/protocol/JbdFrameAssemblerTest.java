package com.gytxtx.openjbd.protocol;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class JbdFrameAssemblerTest {
    @Test
    public void append_reassemblesSplitFrame() {
        JbdFrameAssembler assembler = new JbdFrameAssembler();
        byte[] frame = JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, new byte[]{0x01, 0x02});

        assertTrue(assembler.append(Arrays.copyOfRange(frame, 0, 4)).isEmpty());
        List<JbdFrame> frames = assembler.append(Arrays.copyOfRange(frame, 4, frame.length));

        assertEquals(1, frames.size());
        assertEquals(JbdCommands.CMD_BASIC_INFO & 0xFF, frames.get(0).command);
    }

    @Test
    public void append_extractsMultipleFramesAndSkipsNoise() {
        JbdFrameAssembler assembler = new JbdFrameAssembler();
        byte[] first = JbdFrameTest.responseFrame(JbdCommands.CMD_BASIC_INFO, 0, new byte[]{0x01});
        byte[] second = JbdFrameTest.responseFrame(JbdCommands.CMD_CELL_VOLTAGE, 0, new byte[]{0x0C, (byte) 0xE4});
        byte[] data = concat(new byte[]{0x55, 0x66}, first, second);

        List<JbdFrame> frames = assembler.append(data);

        assertEquals(2, frames.size());
        assertEquals(JbdCommands.CMD_BASIC_INFO & 0xFF, frames.get(0).command);
        assertEquals(JbdCommands.CMD_CELL_VOLTAGE & 0xFF, frames.get(1).command);
    }

    private static byte[] concat(byte[]... chunks) {
        int size = 0;
        for (byte[] chunk : chunks) {
            size += chunk.length;
        }
        byte[] out = new byte[size];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, out, offset, chunk.length);
            offset += chunk.length;
        }
        return out;
    }
}
