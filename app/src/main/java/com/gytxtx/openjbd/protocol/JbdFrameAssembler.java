package com.gytxtx.openjbd.protocol;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class JbdFrameAssembler {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public void reset() {
        buffer.reset();
    }

    public List<JbdFrame> append(byte[] chunk) {
        buffer.write(chunk, 0, chunk.length);
        List<JbdFrame> frames = new ArrayList<>();
        byte[] data = buffer.toByteArray();
        int cursor = 0;
        while (cursor < data.length) {
            while (cursor < data.length && (data[cursor] & 0xFF) != 0xDD) {
                cursor++;
            }
            if (data.length - cursor < 4) {
                break;
            }
            int length = data[cursor + 3] & 0xFF;
            int frameLength = length + 7;
            if (data.length - cursor < frameLength) {
                break;
            }
            byte[] rawFrame = Arrays.copyOfRange(data, cursor, cursor + frameLength);
            try {
                frames.add(JbdFrame.parse(rawFrame));
            } catch (JbdParseException ignored) {
                // Drop malformed bytes and continue looking for the next frame.
            }
            cursor += frameLength;
        }
        buffer.reset();
        if (cursor < data.length) {
            buffer.write(data, cursor, data.length - cursor);
        }
        return frames;
    }
}
