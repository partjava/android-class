package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeatherParser {
    private final Gson gson = new Gson();
    private static final Pattern TEMPERATURE = Pattern.compile("(-?\\d+)\\s*[℃°]");
    public WeatherReport parse(String raw, String expectedCity) throws IOException {
        try {
            Response response = gson.fromJson(raw, Response.class);
            if (response == null || response.status != 200) throw new IOException("天气服务暂不可用");
            if (response.cityInfo == null || !expectedCity.equals(response.cityInfo.citykey))
                throw new IOException("返回的城市与请求不一致");
            if (response.data == null || response.data.forecast == null || response.data.forecast.isEmpty())
                throw new IOException("天气服务未返回预报");
            WeatherReport report = new WeatherReport();
            report.city = required(response.cityInfo.city);
            report.cityCode = expectedCity;
            report.province = fallback(response.cityInfo.parent);
            report.updated = fallback(response.time);
            report.temperature = Double.parseDouble(response.data.wendu);
            if (!Double.isFinite(report.temperature) || Math.abs(report.temperature) > 100)
                throw new IOException("温度数据异常");
            report.humidity = fallback(response.data.shidu);
            report.quality = fallback(response.data.quality);
            report.pm25 = response.data.pm25;
            report.pm10 = response.data.pm10;
            report.advice = fallback(response.data.ganmao);
            String previousDate = "";
            for (Forecast item : response.data.forecast) {
                if (item == null) throw new IOException("预报数据不完整");
                WeatherReport.Day day = new WeatherReport.Day();
                day.date = required(item.ymd);
                LocalDate.parse(day.date);
                if (day.date.compareTo(previousDate) <= 0) throw new IOException("预报日期异常");
                previousDate = day.date;
                day.week = fallback(item.week);
                day.type = required(item.type);
                day.high = temperature(item.high); day.low = temperature(item.low);
                if (day.high < day.low) throw new IOException("最高与最低温度异常");
                day.wind = fallback(item.fx); day.windLevel = fallback(item.fl);
                day.sunrise = fallback(item.sunrise); day.sunset = fallback(item.sunset);
                day.notice = fallback(item.notice); day.aqi = item.aqi;
                report.days.add(day);
            }
            return report;
        } catch (IOException e) { throw e; }
        catch (RuntimeException e) { throw new IOException("天气 JSON 格式错误，请稍后重试", e); }
    }
    private int temperature(String text) throws IOException {
        Matcher matcher = TEMPERATURE.matcher(text == null ? "" : text);
        if (!matcher.find()) throw new IOException("预报温度缺失");
        int value = Integer.parseInt(matcher.group(1));
        if (Math.abs(value) > 100) throw new IOException("预报温度异常");
        return value;
    }
    private String required(String value) throws IOException {
        if (value == null || value.trim().isEmpty()) throw new IOException("天气数据不完整");
        return value;
    }
    private String fallback(String value) { return value == null || value.isEmpty() ? "暂无" : value; }
    private static class Response { int status; String time; CityInfo cityInfo; Data data; }
    private static class CityInfo { String city, citykey, parent; }
    private static class Data { String shidu, quality, wendu, ganmao; Double pm25, pm10; List<Forecast> forecast; }
    private static class Forecast { String ymd, week, high, low, type, fx, fl, sunrise, sunset, notice; Integer aqi; }
}
