package com.example.toutiao.demo.weather;

import org.junit.Test;
import static org.junit.Assert.*;

public class TempBarViewTest {
    @Test public void calculatesCorrectPercentageBounds() {
        // globalMin = 10, globalMax = 30 (range 20)
        // dayLow = 15, dayHigh = 25 (range 10)
        // totalWidth = 200
        // left should be 200 * (15 - 10) / 20 = 50
        // right should be 200 * (25 - 10) / 20 = 150
        float[] bounds = TempBarView.calculateBarBounds(10, 30, 15, 25, 200f, 10f);
        assertEquals(50f, bounds[0], 0.01f);
        assertEquals(150f, bounds[1], 0.01f);
    }

    @Test public void ensuresMinimumBarWidth() {
        // dayLow == dayHigh = 20
        // without min width, left == right == 100
        // with minWidth = 12f, right should be at least left + 12f
        float[] bounds = TempBarView.calculateBarBounds(10, 30, 20, 20, 200f, 12f);
        assertTrue(bounds[1] - bounds[0] >= 12f);
    }

    @Test public void handlesEqualGlobalMinMaxGracefully() {
        float[] bounds = TempBarView.calculateBarBounds(20, 20, 20, 20, 200f, 10f);
        assertEquals(0f, bounds[0], 0.01f);
        assertEquals(200f, bounds[1], 0.01f);
    }
}
