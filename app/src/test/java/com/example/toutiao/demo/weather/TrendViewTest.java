package com.example.toutiao.demo.weather;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class TrendViewTest {
    @Test public void calculatesYCoordinateCorrectly() {
        // min = 10, max = 30, range = 20, top = 20, bottom = 120 (height = 100)
        // temp = 20 -> halfway -> 70
        float y = TrendView.calculateY(20, 10, 30, 20f, 120f);
        assertEquals(70f, y, 0.01f);

        // temp = 30 (max) -> top -> 20
        float yMax = TrendView.calculateY(30, 10, 30, 20f, 120f);
        assertEquals(20f, yMax, 0.01f);

        // temp = 10 (min) -> bottom -> 120
        float yMin = TrendView.calculateY(10, 10, 30, 20f, 120f);
        assertEquals(120f, yMin, 0.01f);
    }

    @Test public void handlesZeroTempRangeGracefully() {
        // When max == min, range is guarded to avoid divide by zero
        float y = TrendView.calculateY(15, 15, 15, 20f, 120f);
        assertFalse(Float.isNaN(y));
        assertFalse(Float.isInfinite(y));
    }
}
