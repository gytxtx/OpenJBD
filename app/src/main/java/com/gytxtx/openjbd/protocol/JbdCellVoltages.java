package com.gytxtx.openjbd.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JbdCellVoltages {
    public final List<Float> cells;
    public final float min;
    public final float max;
    public final float delta;
    public final float average;

    JbdCellVoltages(List<Float> cells) {
        this.cells = Collections.unmodifiableList(new ArrayList<>(cells));
        float localMin = 0f;
        float localMax = 0f;
        float sum = 0f;
        for (float cell : cells) {
            if (localMin == 0f || cell < localMin) {
                localMin = cell;
            }
            if (cell > localMax) {
                localMax = cell;
            }
            sum += cell;
        }
        min = localMin;
        max = localMax;
        delta = localMax - localMin;
        average = cells.isEmpty() ? 0f : sum / cells.size();
    }
}
