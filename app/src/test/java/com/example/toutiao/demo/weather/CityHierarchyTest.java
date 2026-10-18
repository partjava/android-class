package com.example.toutiao.demo.weather;

import org.junit.Test;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.Assert.*;

public class CityHierarchyTest {
    private String readJson() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/beijing.json")) {
            // Test fixture with Wuhan and Beijing
            return "{\"citys\":["
                + "{\"cityName\":\"北京\",\"province\":\"北京\",\"cityPinyin\":\"beijing\",\"cityCode\":\"101010100\"},"
                + "{\"cityName\":\"海淀\",\"province\":\"北京\",\"cityPinyin\":\"haidian\",\"cityCode\":\"101010200\"},"
                + "{\"cityName\":\"朝阳\",\"province\":\"北京\",\"cityPinyin\":\"chaoyang\",\"cityCode\":\"101010300\"},"
                + "{\"cityName\":\"武汉\",\"province\":\"湖北\",\"cityPinyin\":\"wuhan\",\"cityCode\":\"101200101\"},"
                + "{\"cityName\":\"江夏\",\"province\":\"湖北\",\"cityPinyin\":\"jiangxia\",\"cityCode\":\"101200105\"},"
                + "{\"cityName\":\"蔡甸\",\"province\":\"湖北\",\"cityPinyin\":\"caidian\",\"cityCode\":\"101200102\"}"
                + "]}";
        }
    }

    @Test public void separatesMainCityFromDistricts() throws Exception {
        CityCatalog catalog = new CityCatalog(readJson());

        CityCatalog.City wuhan = catalog.byCode("101200101");
        assertTrue(wuhan.isMainCity);
        assertEquals("武汉", wuhan.name);
        assertEquals("武汉", wuhan.parentCity);

        CityCatalog.City jiangxia = catalog.byCode("101200105");
        assertFalse(jiangxia.isMainCity);
        assertEquals("江夏", jiangxia.name);
        assertEquals("武汉", jiangxia.parentCity);
        assertEquals("湖北", jiangxia.province);

        List<CityCatalog.City> wuhanDistricts = catalog.getDistrictsOf("武汉");
        assertEquals(2, wuhanDistricts.size());
        assertTrue(wuhanDistricts.stream().anyMatch(c -> c.name.equals("江夏")));
        assertTrue(wuhanDistricts.stream().anyMatch(c -> c.name.equals("蔡甸")));
    }

    @Test public void handlesMunicipalitiesCorrectly() throws Exception {
        CityCatalog catalog = new CityCatalog(readJson());

        CityCatalog.City beijing = catalog.byCode("101010100");
        assertTrue(beijing.isMainCity);

        CityCatalog.City haidian = catalog.byCode("101010200");
        assertFalse(haidian.isMainCity);
        assertEquals("北京", haidian.parentCity);
    }

    @Test public void searchingMainCityIncludesItsDistricts() throws Exception {
        CityCatalog catalog = new CityCatalog(readJson());
        List<CityCatalog.City> results = catalog.search("武汉");

        // First result should be Wuhan main city
        assertEquals("101200101", results.get(0).code);
        assertTrue(results.get(0).isMainCity);

        // Subsequent results include Wuhan's districts
        assertTrue(results.stream().anyMatch(c -> c.name.equals("江夏")));
    }
}
