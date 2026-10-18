package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;

public final class CityCatalog {
    public static final class City {
        public final String name;
        public final String province;
        public final String pinyin;
        public final String code;
        public final String parentCity;
        public final boolean isMainCity;

        public City(String name, String province, String pinyin, String code, String parentCity, boolean isMainCity) {
            this.name = name;
            this.province = province;
            this.pinyin = pinyin;
            this.code = code;
            this.parentCity = parentCity;
            this.isMainCity = isMainCity;
        }

        public String getDisplayName() {
            if (isMainCity) {
                return name + (name.endsWith("市") ? "" : "市");
            } else {
                return parentCity + " · " + name + (name.endsWith("区") || name.endsWith("县") || name.endsWith("市") ? "" : "区");
            }
        }

        public String getLocationSummary() {
            if (isMainCity) {
                return province + (province.endsWith("省") || province.endsWith("市") ? "" : "省") + " · 地级市/主城区";
            } else {
                return province + " · " + parentCity + "市下辖区县";
            }
        }

        @Override public String toString() {
            return getDisplayName() + "  ·  " + province;
        }
    }

    private final List<City> allCities = new ArrayList<>();
    private final List<City> mainCities = new ArrayList<>();
    private final Map<String, List<City>> districtsByParent = new LinkedHashMap<>();
    private final Map<String, City> byCodeMap = new HashMap<>();

    public CityCatalog(String json) throws JSONException {
        JSONArray rows = new JSONObject(json).getJSONArray("citys");
        List<RawEntry> rawList = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String code = row.getString("cityCode");
            if (code.matches("\\d{9}")) {
                rawList.add(new RawEntry(
                    row.getString("cityName"),
                    row.optString("province"),
                    row.optString("cityPinyin"),
                    code
                ));
            }
        }

        // 1. Determine parent city for each group
        Map<String, String> parentCityMap = new HashMap<>();
        for (RawEntry entry : rawList) {
            if (isMunicipality(entry.province)) {
                if (entry.code.endsWith("0100")) {
                    parentCityMap.put(entry.province, entry.name);
                }
            } else {
                String pref = entry.code.substring(0, 7);
                if (entry.code.endsWith("01") || !parentCityMap.containsKey(pref)) {
                    parentCityMap.put(pref, entry.name);
                }
            }
        }

        // 2. Build City models with parent and isMainCity flags
        for (RawEntry entry : rawList) {
            String parent;
            boolean isMain;
            if (isMunicipality(entry.province)) {
                parent = entry.province;
                isMain = entry.code.endsWith("0100");
            } else {
                String pref = entry.code.substring(0, 7);
                parent = parentCityMap.getOrDefault(pref, entry.name);
                isMain = entry.code.endsWith("01") || entry.name.equals(parent);
            }

            City city = new City(entry.name, entry.province, entry.pinyin, entry.code, parent, isMain);
            allCities.add(city);
            byCodeMap.put(city.code, city);
            if (isMain) {
                mainCities.add(city);
            }
            districtsByParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(city);
        }
    }

    private static boolean isMunicipality(String province) {
        return "北京".equals(province) || "上海".equals(province)
            || "天津".equals(province) || "重庆".equals(province);
    }

    public List<City> getMainCities() {
        return new ArrayList<>(mainCities);
    }

    public List<City> getDistrictsOf(String parentCityName) {
        List<City> list = districtsByParent.get(parentCityName);
        if (list == null) return Collections.emptyList();
        List<City> sub = new ArrayList<>();
        for (City c : list) {
            if (!c.isMainCity) sub.add(c);
        }
        return sub;
    }

    public List<City> search(String input) {
        String query = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return new ArrayList<>(allCities.subList(0, Math.min(100, allCities.size())));
        }
        List<City> result = new ArrayList<>();
        for (City city : allCities) {
            if (matches(city, query)) {
                result.add(city);
            }
        }
        result.sort((a, b) -> rank(a, query) - rank(b, query));
        return result.size() > 100 ? new ArrayList<>(result.subList(0, 100)) : result;
    }

    private boolean matches(City city, String query) {
        return city.name.contains(query)
            || city.province.contains(query)
            || city.parentCity.contains(query)
            || city.pinyin.toLowerCase(Locale.ROOT).contains(query)
            || city.code.contains(query);
    }

    private int rank(City city, String query) {
        String pinyin = city.pinyin.toLowerCase(Locale.ROOT);
        // Exact name or code match
        if (city.name.equals(query) || pinyin.equals(query) || city.code.equals(query)) {
            return city.isMainCity ? 0 : 2;
        }
        // Starts with name or pinyin
        if (city.name.startsWith(query) || pinyin.startsWith(query) || city.code.startsWith(query)) {
            return city.isMainCity ? 1 : 3;
        }
        // Belong to queried parent city (e.g. searched 武汉, matches 江夏)
        if (city.parentCity.equals(query)) {
            return 4;
        }
        return city.isMainCity ? 5 : 6;
    }

    public City byCode(String code) {
        City c = byCodeMap.get(code);
        return c != null ? c : allCities.get(0);
    }

    private static final class RawEntry {
        final String name, province, pinyin, code;
        RawEntry(String name, String province, String pinyin, String code) {
            this.name = name; this.province = province; this.pinyin = pinyin; this.code = code;
        }
    }
}
