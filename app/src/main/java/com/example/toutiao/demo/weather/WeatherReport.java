package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import java.util.ArrayList;
import java.util.List;

/** UI model produced only after the complete response has been validated. */
public final class WeatherReport {
    public String city, cityCode, province, updated, humidity, quality, advice;
    public double temperature;
    public Double pm25, pm10;
    public final List<Day> days = new ArrayList<>();
    public static final class Day {
        public String date, week, type, wind, windLevel, sunrise, sunset, notice;
        public int high, low;
        public Integer aqi;
    }
}
