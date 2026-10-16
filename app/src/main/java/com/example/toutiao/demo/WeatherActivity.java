package com.example.toutiao.demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

/** ECharts 天气曲线（混合开发演示）：本地 weather.html + echarts.min.js，
 * 数据来自 FastAPI /api/v1/weather，失败时用内置 mock，页面本身不联网。 */
public class WeatherActivity extends AppCompatActivity {
    private WebView web;
    private boolean pageReady;
    private boolean rendered;
    private JSONObject pending;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        findViewById(R.id.iv_weather_back).setOnClickListener(v -> finish());

        web = findViewById(R.id.weather_web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);   // 页面只用本地资源，联网由 Java 端负责
        web.setWebChromeClient(new android.webkit.WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;   // 禁止页内跳转
            }
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                if (pending != null) render(pending);   // 数据先到、页面后好的时序补偿
                else fetchAndRender();
            }
        });
        web.loadUrl("file:///android_asset/echarts/weather.html");
    }

    private void fetchAndRender() {
        ApiClient.get(CampusNetworkConfig.getApiBaseUrl() + "/weather", new ApiClient.Callback() {
            @Override public void onSuccess(JSONObject response) {
                JSONObject data = response.optJSONObject("data");
                boolean ok = response.optInt("code") == 200
                        && data != null && data.optJSONArray("days") != null && data.optJSONArray("days").length() > 0;
                render(ok ? data : bundledMock());
            }
            @Override public void onError(String message) {
                render(bundledMock());
            }
        });
    }

    private void render(JSONObject data) {
        if (web == null || isDestroyed() || isFinishing()) return;
        if (!pageReady) {
            pending = data;   // echarts.min.js 尚未就绪，等 onPageFinished 再注入
            return;
        }
        if (rendered) return;
        rendered = true;
        web.evaluateJavascript("window.renderWeather && window.renderWeather(" + data + ")", null);
    }

    /** 离线兜底：与后端 _weather_mock 同构的 7 天演示数据。 */
    private JSONObject bundledMock() {
        JSONObject data = new JSONObject();
        JSONArray days = new JSONArray();
        String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        String[] conditions = {"晴", "多云", "阴", "小雨"};
        Calendar calendar = Calendar.getInstance();
        try {
            data.put("city", "武汉");
            data.put("updated", "本地演示数据");
            for (int i = 0; i < 7; i++) {
                JSONObject day = new JSONObject();
                day.put("date", String.format(java.util.Locale.CHINA, "%02d-%02d",
                        calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)));
                day.put("week", i == 0 ? "今天" : weeks[calendar.get(Calendar.DAY_OF_WEEK) - 1]);
                day.put("high", 22 + (i * 3) % 4);
                day.put("low", 9 + (i * 2) % 8);
                day.put("cond", conditions[i % conditions.length]);
                days.put(day);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            data.put("days", days);
        } catch (Exception ignored) {
        }
        return data;
    }

    @Override protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override protected void onDestroy() {
        if (web != null) {
            ((android.view.ViewGroup) web.getParent()).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
