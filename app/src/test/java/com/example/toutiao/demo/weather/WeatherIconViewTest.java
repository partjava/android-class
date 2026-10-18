package com.example.toutiao.demo.weather;

import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherIconViewTest {
    @Test public void normalizesVariousWeatherTypes() {
        assertEquals("晴", WeatherIconView.normalizeType("晴"));
        assertEquals("多云", WeatherIconView.normalizeType("晴间多云"));
        assertEquals("阴", WeatherIconView.normalizeType("阴天"));
        assertEquals("雨", WeatherIconView.normalizeType("小雨转中雨"));
        assertEquals("雷", WeatherIconView.normalizeType("雷阵雨"));
        assertEquals("雪", WeatherIconView.normalizeType("暴雪"));
        assertEquals("雾", WeatherIconView.normalizeType("大雾"));
        assertEquals("多云", WeatherIconView.normalizeType(null));
    }

    @Test public void classifiesSunAndRainCorrectly() {
        assertTrue(WeatherIconView.isRainy("阵雨"));
        assertTrue(WeatherIconView.isRainy("大雨到暴雨"));
        assertFalse(WeatherIconView.isRainy("晴"));
        assertTrue(WeatherIconView.isSnowy("小雪"));
        assertTrue(WeatherIconView.isThunder("强雷电天气"));
    }
}
