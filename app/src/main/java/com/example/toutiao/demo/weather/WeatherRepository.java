package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

/** Called on the worker executor, never the Android UI thread. */
public final class WeatherRepository {
    public static final String BASE_URL = "http://t.weather.sojson.com/api/weather/city/";
    private final OkHttpClient client;
    private final JsonCache cache;
    private final String baseUrl;
    private final WeatherParser parser = new WeatherParser();
    private volatile Call active;
    public static final class Result {
        public final WeatherReport report;
        public final boolean cached;
        public final long savedAt;
        public final String note;
        Result(WeatherReport report, boolean cached, long savedAt, String note) {
            this.report = report; this.cached = cached; this.savedAt = savedAt; this.note = note;
        }
    }
    public WeatherRepository(OkHttpClient client, JsonCache cache, String baseUrl) {
        this.client = client; this.cache = cache; this.baseUrl = baseUrl;
    }
    public void cancel() { Call call = active; if (call != null) call.cancel(); }
    public Result cached(String city) throws IOException {
        String raw = cache.read(city);
        return raw == null ? null : new Result(parser.parse(raw, city), true, cache.savedAt(city), "已载入本地天气");
    }
    public Result fetch(String city) throws IOException {
        if (city == null || !city.matches("\\d{9}")) throw new IOException("城市编码无效");
        Call call = client.newCall(new Request.Builder().url(baseUrl + city).get()
            .header("Accept", "application/json").build());
        active = call;
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) throw new IOException("天气服务 HTTP " + response.code());
            if (response.body() == null) throw new IOException("天气服务返回空内容");
            String raw = response.body().string();
            WeatherReport report = parser.parse(raw, city);
            try {
                cache.write(city, raw);
                // Display is parsed from the file, completing the lab's save-then-parse flow.
                return new Result(parser.parse(cache.read(city), city), false, cache.savedAt(city), "");
            } catch (IOException storage) {
                return new Result(report, false, 0, "天气已更新，但本地保存失败");
            }
        } catch (IOException network) {
            if (call.isCanceled()) throw new IOException("请求已取消", network);
            try {
                Result local = cached(city);
                if (local != null) return new Result(local.report, true, local.savedAt,
                    "更新失败，正在显示本地缓存");
            } catch (IOException invalidCache) { network.addSuppressed(invalidCache); }
            throw new IOException("暂时无法获取天气，请检查网络后重试", network);
        } finally { if (active == call) active = null; }
    }
}
