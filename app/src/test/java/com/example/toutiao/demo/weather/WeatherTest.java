package com.example.toutiao.demo.weather;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class WeatherTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private String fixture() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/beijing.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    @Test public void parsesActualWeatherAndFifteenDays() throws Exception {
        WeatherReport report = new WeatherParser().parse(fixture(), "101010100");
        assertEquals("北京市", report.city);
        assertEquals(15, report.days.size());
        assertTrue(report.days.get(0).high >= report.days.get(0).low);
        assertFalse(report.days.get(0).date.isEmpty());
    }
    @Test public void rejectsServiceFailure() throws Exception {
        try { new WeatherParser().parse("{\"status\":403}", "101010100"); fail(); }
        catch (IOException expected) { assertTrue(expected.getMessage().contains("服务")); }
    }
    @Test public void rejectsWrongCity() throws Exception {
        try { new WeatherParser().parse(fixture(), "101020100"); fail(); }
        catch (IOException expected) { assertTrue(expected.getMessage().contains("城市")); }
    }
    @Test public void rejectsMalformedJson() throws Exception {
        try { new WeatherParser().parse("not json", "101010100"); fail(); }
        catch (IOException expected) { assertNotNull(expected.getMessage()); }
    }
    @Test public void rejectsInvalidTemperatureAndEmptyForecast() throws Exception {
        String raw = fixture().replaceFirst("高温 [-0-9]+℃", "高温 无数据");
        try { new WeatherParser().parse(raw, "101010100"); fail(); }
        catch (IOException expected) { assertNotNull(expected.getMessage()); }
        try { new WeatherParser().parse("{\"status\":200,\"cityInfo\":{\"citykey\":\"101010100\",\"city\":\"北京\"},\"data\":{\"wendu\":\"12\",\"forecast\":[]}}", "101010100"); fail(); }
        catch (IOException expected) { assertNotNull(expected.getMessage()); }
    }
    @Test public void cachePreservesRawJsonAndSeparatesCities() throws Exception {
        JsonCache cache = new JsonCache(folder.newFolder());
        cache.write("101010100", fixture());
        assertEquals(fixture(), cache.read("101010100"));
        assertNull(cache.read("101020100"));
        assertTrue(cache.savedAt("101010100") > 0);
        cache.write("101010100", "更新");
        assertEquals("更新", cache.read("101010100"));
    }
    @Test public void searchesCityNamePinyinAndCode() throws Exception {
        CityCatalog catalog = new CityCatalog("{\"citys\":[{\"cityName\":\"北京\",\"province\":\"北京\",\"cityPinyin\":\"beijing\",\"cityCode\":\"101010100\"},{\"cityName\":\"南京\",\"province\":\"江苏\",\"cityPinyin\":\"nanjing\",\"cityCode\":\"101190101\"}]}");
        assertEquals("101010100", catalog.search("北京").get(0).code);
        assertEquals("101010100", catalog.search("BEIJING").get(0).code);
        assertEquals("南京", catalog.search("101190101").get(0).name);
        assertEquals(0, catalog.search("不存在").size());
    }
    @Test public void exactCityMatchRanksBeforeSubstring() throws Exception {
        CityCatalog catalog = new CityCatalog("{\"citys\":[{\"cityName\":\"常州\",\"province\":\"江苏\",\"cityPinyin\":\"changzhou\",\"cityCode\":\"101191101\"},{\"cityName\":\"杭州\",\"province\":\"浙江\",\"cityPinyin\":\"hangzhou\",\"cityCode\":\"101210101\"}]}");
        assertEquals("杭州", catalog.search("hangzhou").get(0).name);
    }
}
