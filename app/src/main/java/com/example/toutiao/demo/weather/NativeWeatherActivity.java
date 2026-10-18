package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import okhttp3.OkHttpClient;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

public class NativeWeatherActivity extends Activity {
    private static final int INK = 0xFF172B53;
    private static final int SUB_INK = 0xFF334155;
    private static final int MUTED = 0xFF64748B;
    private static final int BLUE = 0xFF3B82F6;
    private static final int BORDER = 0xFFE2E8F0;

    private LinearLayout content;
    private TextView cityLabel, refresh;
    private View cityContainer;
    private ScrollView scroll;
    private CityCatalog catalog;
    private CityCatalog.City city;
    private WeatherRepository repository;
    private JsonCache cache;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile int generation;
    private volatile boolean destroyed;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_weather_native);
        findViewById(R.id.weather_back).setOnClickListener(v -> finish());

        content = findViewById(R.id.weather_content);
        cityLabel = findViewById(R.id.weather_city);
        refresh = findViewById(R.id.weather_refresh);
        cityContainer = findViewById(R.id.weather_city_container);
        scroll = findViewById(R.id.weather_scroll);

        refresh.setBackground(bg(BLUE, 18));
        cityContainer.setBackground(bg(0x1F3B82F6, 18));

        prefs = getSharedPreferences("native_weather_v1", MODE_PRIVATE);
        cache = new JsonCache(new File(getFilesDir(), "weather"));
        repository = new WeatherRepository(
            new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build(),
            cache,
            WeatherRepository.BASE_URL
        );

        cityContainer.setOnClickListener(v -> { if (catalog != null) chooseCity(); });
        refresh.setOnClickListener(v -> { if (city != null) load(); });

        showEmpty("正在准备天气", "每一段日常，都有好天气。", false);

        worker.execute(() -> {
            try {
                String raw;
                try (InputStream in = getAssets().open("weather/cities.json")) {
                    raw = readUtf8(in);
                }
                CityCatalog loaded = new CityCatalog(raw);
                runOnUiThread(() -> {
                    if (destroyed) return;
                    catalog = loaded;
                    String savedCity = prefs.getString("city", "101200101");
                    city = catalog.byCode(savedCity);
                    if (!city.code.equals(savedCity)) city = catalog.byCode("101200101");
                    load();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!destroyed) showEmpty("城市资料读取失败", "请重新安装应用后重试。", false);
                });
            }
        });
    }

    private void load() {
        final int token = ++generation;
        final CityCatalog.City selected = city;
        repository.cancel();

        // Display hierarchy: "武汉 · 江夏" for district, "武汉" for main city
        cityLabel.setText(selected.getDisplayName());
        prefs.edit().putString("city", selected.code).apply();
        refresh.setEnabled(false);
        refresh.setText("更新中…");
        refresh.setBackground(bg(0xFF94A3B8, 18));
        scroll.scrollTo(0, 0);

        showEmpty("正在获取 " + selected.getDisplayName() + " 天气", "正在连接气象服务…", false);

        worker.execute(() -> {
            if (destroyed || token != generation) return;
            try {
                WeatherRepository.Result saved = repository.cached(selected.code);
                if (saved != null) {
                    runOnUiThread(() -> {
                        if (!destroyed && token == generation) render(saved, true);
                    });
                }
            } catch (IOException ignored) {}

            if (destroyed || token != generation) return;
            try {
                WeatherRepository.Result result = repository.fetch(selected.code);
                runOnUiThread(() -> {
                    if (!destroyed && token == generation) {
                        render(result, false);
                        finishLoading();
                    }
                });
            } catch (IOException error) {
                runOnUiThread(() -> {
                    if (!destroyed && token == generation) {
                        showEmpty("暂时没有天气数据", error.getMessage() + "\n首次使用需要联网，成功后可离线查看。", true);
                        finishLoading();
                    }
                });
            }
        });
    }

    private void finishLoading() {
        refresh.setEnabled(true);
        refresh.setText("刷新");
        refresh.setBackground(bg(BLUE, 18));
    }

    private void render(WeatherRepository.Result result, boolean updating) {
        content.removeAllViews();
        WeatherReport report = result.report;
        WeatherReport.Day day = report.days.get(0);
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        boolean outdated = !day.date.equals(today);

        // 1. Hero Weather Card
        LinearLayout hero = card();
        hero.setPadding(dp(22), dp(20), dp(22), dp(22));
        hero.setBackground(gradientCard(0xFFFFFFFF, 0xFFF6F9FD, 24));

        // Location & Administrative Hierarchy Badge Row
        LinearLayout locationRow = row();
        locationRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView locBadge = text(city.getLocationSummary(), 11, 0xFF475569, true);
        locBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        locBadge.setBackground(bg(0xFFF1F5F9, 12));
        locationRow.addView(locBadge);

        String stateText = updating ? "● 正在更新"
            : (result.cached ? "● 离线缓存 (" + time(result.savedAt) + ")"
            : "● 已更新 " + report.updated);
        int stateColor = updating ? BLUE : (result.cached ? 0xFFD97706 : 0xFF10B981);
        int stateBg = updating ? 0x1A3B82F6 : (result.cached ? 0x22F59E0B : 0x1F10B981);
        TextView stateBadge = text(stateText, 11, stateColor, true);
        stateBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        stateBadge.setBackground(bg(stateBg, 12));
        LinearLayout.LayoutParams stateLp = new LinearLayout.LayoutParams(-2, -2);
        stateLp.leftMargin = dp(8);
        locationRow.addView(stateBadge, stateLp);
        hero.addView(locationRow);

        if (outdated) {
            TextView warnBadge = text("数据非今日，请联网刷新", 11, 0xFFDC2626, false);
            warnBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
            warnBadge.setBackground(bg(0x1CDC2626, 12));
            margin(warnBadge, 8);
            hero.addView(warnBadge);
        }

        // Main Temperature Reading Row
        LinearLayout mainRow = row();
        mainRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout reading = vertical();

        TextView temperature = text(String.format(Locale.CHINA, "%.0f°", report.temperature), 76, INK, false);
        temperature.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        temperature.setIncludeFontPadding(false);
        reading.addView(temperature);

        TextView typeAndRange = text(day.type + "   " + day.low + "° ~ " + day.high + "°", 17, SUB_INK, true);
        margin(typeAndRange, 4);
        reading.addView(typeAndRange);

        mainRow.addView(reading, new LinearLayout.LayoutParams(0, -2, 1));
        mainRow.addView(new WeatherIconView(this, day.type), new LinearLayout.LayoutParams(dp(120), dp(120)));
        margin(mainRow, 14);
        hero.addView(mainRow);

        // Air Quality & Wind Badges
        LinearLayout badgeRow = row();
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        int aqi = day.aqi == null ? 0 : day.aqi;
        int aqiBg = aqi <= 50 ? 0xFFDCFCE7 : (aqi <= 100 ? 0xFFFEF9C3 : 0xFFFFEDD5);
        int aqiColor = aqi <= 50 ? 0xFF15803D : (aqi <= 100 ? 0xFF854D0E : 0xFFC2410C);
        String aqiLabel = "🍃 空气 " + report.quality + (day.aqi == null ? "" : " (" + day.aqi + ")");
        TextView aqiBadge = text(aqiLabel, 12, aqiColor, true);
        aqiBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        aqiBadge.setBackground(bg(aqiBg, 14));
        badgeRow.addView(aqiBadge);

        TextView windBadge = text("💨 " + day.wind + " " + day.windLevel, 12, 0xFF0369A1, false);
        windBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        windBadge.setBackground(bg(0xFFE0F2FE, 14));
        LinearLayout.LayoutParams windLp = new LinearLayout.LayoutParams(-2, -2);
        windLp.leftMargin = dp(8);
        badgeRow.addView(windBadge, windLp);

        margin(badgeRow, 14);
        hero.addView(badgeRow);

        // Notice Quote
        if (day.notice != null && !day.notice.isEmpty()) {
            TextView notice = text("💬 " + day.notice, 13, 0xFF475569, false);
            notice.setPadding(dp(12), dp(8), dp(12), dp(8));
            notice.setBackground(borderedBg(0xFFF8FAFC, BORDER, 14));
            margin(notice, 14);
            hero.addView(notice);
        }

        addCard(hero);

        if (!result.note.isEmpty() && !updating) {
            TextView note = text(result.note, 12, 0xFFB45309, false);
            note.setPadding(dp(20), 0, dp(20), dp(8));
            content.addView(note);
        }

        // 2. Key Metrics Grid
        LinearLayout glance = card();
        addHeading(glance, outdated ? "天气概览" : "今日气象指标", day.date.substring(5).replace('-', '/') + "  " + day.week);

        LinearLayout gridRow1 = row();
        margin(gridRow1, 14);
        gridRow1.addView(metricBox("相对湿度", report.humidity, "💧 水汽充足"), new LinearLayout.LayoutParams(0, -2, 1));
        View space1 = new View(this);
        gridRow1.addView(space1, new LinearLayout.LayoutParams(dp(10), 1));
        String pmVal = report.pm25 == null ? "暂无" : String.format(Locale.CHINA, "%.0f", report.pm25) + " μg";
        gridRow1.addView(metricBox("细颗粒物", pmVal, "🌫️ PM2.5 监测"), new LinearLayout.LayoutParams(0, -2, 1));
        glance.addView(gridRow1);

        LinearLayout gridRow2 = row();
        margin(gridRow2, 10);
        String aqiVal = day.aqi == null ? "暂无" : day.aqi + " · " + report.quality;
        gridRow2.addView(metricBox("空气质量指数", aqiVal, "🌱 整体环境"), new LinearLayout.LayoutParams(0, -2, 1));
        View space2 = new View(this);
        gridRow2.addView(space2, new LinearLayout.LayoutParams(dp(10), 1));
        gridRow2.addView(metricBox("日出 / 日落", day.sunrise + " / " + day.sunset, "🌅 昼夜交替"), new LinearLayout.LayoutParams(0, -2, 1));
        glance.addView(gridRow2);

        addCard(glance);

        // 3. Trend View Card
        LinearLayout trend = card();
        addHeading(trend, "温度趋势", "未来 " + Math.min(7, report.days.size()) + " 天 · 高低温趋势");

        LinearLayout legendRow = row();
        legendRow.setGravity(Gravity.CENTER_VERTICAL);
        margin(legendRow, 8);
        TextView legendHigh = text("● 最高温", 11, 0xFFF59E0B, true);
        TextView legendLow = text("● 最低温", 11, 0xFF3B82F6, true);
        legendRow.addView(legendHigh);
        LinearLayout.LayoutParams lowLp = new LinearLayout.LayoutParams(-2, -2);
        lowLp.leftMargin = dp(16);
        legendRow.addView(legendLow, lowLp);
        trend.addView(legendRow);

        TrendView trendView = new TrendView(this, report.days);
        margin(trendView, 10);
        trend.addView(trendView, new LinearLayout.LayoutParams(-1, dp(185)));
        addCard(trend);

        // 4. 15-Day Forecast Card with Temp Bar
        LinearLayout forecast = card();
        addHeading(forecast, report.days.size() + " 天逐日预报", "含冷热温差区间 · 点选详情");

        int globalMin = 100, globalMax = -100;
        for (WeatherReport.Day d : report.days) {
            globalMin = Math.min(globalMin, d.low);
            globalMax = Math.max(globalMax, d.high);
        }

        for (int i = 0; i < report.days.size(); i++) {
            WeatherReport.Day forecastDay = report.days.get(i);
            LinearLayout item = row();
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(4), dp(10), dp(4), dp(10));

            LinearLayout dateCol = vertical();
            String name = forecastDay.date.equals(today) ? "今天" : forecastDay.week.replace("星期", "周");
            dateCol.addView(text(name, 14, INK, forecastDay.date.equals(today)));
            dateCol.addView(text(forecastDay.date.substring(5).replace('-', '/'), 11, MUTED, false));
            item.addView(dateCol, new LinearLayout.LayoutParams(dp(54), -2));

            item.addView(new WeatherIconView(this, forecastDay.type), new LinearLayout.LayoutParams(dp(36), dp(36)));

            TextView type = text(forecastDay.type, 12, SUB_INK, false);
            type.setGravity(Gravity.CENTER);
            item.addView(type, new LinearLayout.LayoutParams(dp(48), -2));

            TextView lowText = text(forecastDay.low + "°", 13, MUTED, false);
            lowText.setGravity(Gravity.END);
            item.addView(lowText, new LinearLayout.LayoutParams(dp(30), -2));

            TempBarView bar = new TempBarView(this);
            bar.setRange(globalMin, globalMax, forecastDay.low, forecastDay.high);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(0, dp(6), 1);
            barLp.leftMargin = dp(10);
            barLp.rightMargin = dp(10);
            item.addView(bar, barLp);

            TextView highText = text(forecastDay.high + "°", 13, INK, true);
            highText.setGravity(Gravity.START);
            item.addView(highText, new LinearLayout.LayoutParams(dp(30), -2));

            item.setBackground(selectable());
            item.setOnClickListener(v -> dayDetail(forecastDay));
            forecast.addView(item);

            if (i < report.days.size() - 1) {
                View separator = new View(this);
                separator.setBackgroundColor(0xFFF1F5F9);
                forecast.addView(separator, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        addCard(forecast);

        // 5. Living Tip Card
        LinearLayout tip = card();
        addHeading(tip, "生活提醒", "贴心天气指数");
        TextView advice = text(report.advice, 13, 0xFF475569, false);
        advice.setLineSpacing(dp(5), 1);
        margin(advice, 12);
        tip.addView(advice);
        addCard(tip);

        // Data source and cache age remain visible in the forecast.
        TextView footer = text("晴序天气 · 把日子过成好天气\n数据来源：sojson 气象接口 · 预报仅供参考", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(6), 1);
        content.addView(footer);
    }

    private void dayDetail(WeatherReport.Day day) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = vertical();
        card.setPadding(dp(24), dp(24), dp(24), dp(20));
        card.setBackground(borderedBg(Color.WHITE, BORDER, 24));

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(new WeatherIconView(this, day.type), new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout headerText = vertical();
        headerText.setPadding(dp(14), 0, 0, 0);
        headerText.addView(text(day.date + " · " + day.week, 16, INK, true));
        headerText.addView(text(day.type + "   " + day.low + "℃ ~ " + day.high + "℃", 14, BLUE, true));
        top.addView(headerText, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(top);

        View div = new View(this);
        div.setBackgroundColor(0xFFF1F5F9);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(-1, dp(1));
        divLp.topMargin = dp(16);
        divLp.bottomMargin = dp(16);
        card.addView(div, divLp);

        LinearLayout row1 = row();
        row1.addView(detailItem("风向风力", day.wind + " " + day.windLevel), new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(detailItem("空气质量指数", day.aqi == null ? "暂无" : day.aqi.toString()), new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row1);

        LinearLayout row2 = row();
        margin(row2, 10);
        row2.addView(detailItem("日出时间", day.sunrise), new LinearLayout.LayoutParams(0, -2, 1));
        row2.addView(detailItem("日落时间", day.sunset), new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row2);

        if (day.notice != null && !day.notice.isEmpty()) {
            TextView notice = text("提示：" + day.notice, 12, 0xFF475569, false);
            notice.setPadding(dp(12), dp(10), dp(12), dp(10));
            notice.setBackground(bg(0xFFF8FAFC, 12));
            margin(notice, 16);
            card.addView(notice);
        }

        Button ok = new Button(this);
        ok.setText("知道了");
        ok.setTextColor(Color.WHITE);
        ok.setBackground(bg(BLUE, 16));
        ok.setOnClickListener(v -> dialog.dismiss());
        margin(ok, 18);
        card.addView(ok);

        dialog.setContentView(card, new ViewGroup.LayoutParams(dp(320), -2));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private LinearLayout detailItem(String label, String value) {
        LinearLayout item = vertical();
        item.addView(text(label, 11, MUTED, false));
        TextView val = text(value, 14, INK, true);
        margin(val, 2);
        item.addView(val);
        return item;
    }

    private void showLocalData(String code) {
        worker.execute(() -> {
            String message;
            try {
                String raw = cache.read(code);
                message = "【实验二数据链路说明】\n"
                    + "OkHttp 网络请求 ➔ UTF-8 原始 JSON 文件写入 ➔ Gson 解析天气数据 ➔ UI 现代渲染展示。\n"
                    + "城市表基于 org.json 独立解析检索。\n\n"
                    + "请求接口：\n" + WeatherRepository.BASE_URL + code + "\n\n"
                    + "本地缓存：files/weather/" + code + ".json\n"
                    + "保存时间：" + time(cache.savedAt(code)) + "\n\n"
                    + "原始响应预览：\n" + (raw == null ? "暂无本地数据" : raw);
            } catch (IOException e) {
                message = "读取本地文件失败：" + e.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> {
                if (destroyed) return;
                Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                LinearLayout card = vertical();
                card.setPadding(dp(22), dp(22), dp(22), dp(16));
                card.setBackground(borderedBg(Color.WHITE, BORDER, 24));

                TextView title = text("本地天气数据与说明", 17, INK, true);
                card.addView(title);

                ScrollView panel = new ScrollView(this);
                TextView raw = text(result, 12, SUB_INK, false);
                raw.setTypeface(Typeface.MONOSPACE);
                raw.setTextIsSelectable(true);
                raw.setLineSpacing(dp(4), 1);
                raw.setPadding(dp(12), dp(12), dp(12), dp(12));
                raw.setBackground(bg(0xFFF8FAFC, 14));
                panel.addView(raw);

                LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(-1, dp(340));
                panelLp.topMargin = dp(14);
                card.addView(panel, panelLp);

                Button ok = new Button(this);
                ok.setText("关闭");
                ok.setTextColor(Color.WHITE);
                ok.setBackground(bg(BLUE, 16));
                ok.setOnClickListener(v -> dialog.dismiss());
                margin(ok, 14);
                card.addView(ok);

                dialog.setContentView(card, new ViewGroup.LayoutParams(dp(330), -2));
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
                dialog.show();
            });
        });
    }

    /**
     * Modern Hierarchical City Picker:
     * Separates Prefecture/Main Cities from Sub-districts/Counties!
     */
    private void chooseCity() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout body = vertical();
        body.setPadding(dp(20), dp(20), dp(20), dp(16));
        body.setBackground(borderedBg(Color.WHITE, BORDER, 24));

        // Header
        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("选择城市与市区", 18, INK, true);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = text("✕", 18, MUTED, false);
        close.setPadding(dp(8), dp(4), dp(8), dp(4));
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        body.addView(head);

        // Search Input
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("🔍 搜城市名 / 区县名 / 拼音 (如: 武汉、江夏)");
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        search.setBackground(borderedBg(0xFFF8FAFC, BORDER, 14));
        margin(search, 14);
        body.addView(search);

        // Subtitle indicator
        TextView note = text("💡 支持精准区分地级市与下辖区县 (如武汉市 vs 江夏区)", 11, 0xFF3B82F6, false);
        margin(note, 8);
        body.addView(note);

        // Category Tabs: [主要地级市 (347个)] | [全部与区县]
        LinearLayout tabs = row();
        margin(tabs, 10);
        TextView tabMain = text("主要地级市", 12, Color.WHITE, true);
        tabMain.setPadding(dp(14), dp(6), dp(14), dp(6));
        tabMain.setBackground(bg(BLUE, 14));

        TextView tabAll = text("全部与下辖区县", 12, MUTED, false);
        tabAll.setPadding(dp(14), dp(6), dp(14), dp(6));
        tabAll.setBackground(borderedBg(0xFFF8FAFC, BORDER, 14));
        LinearLayout.LayoutParams allTabLp = new LinearLayout.LayoutParams(-2, -2);
        allTabLp.leftMargin = dp(8);

        tabs.addView(tabMain);
        tabs.addView(tabAll, allTabLp);
        body.addView(tabs);

        // Popular Hot Cities Chips Section
        LinearLayout hotSection = vertical();
        margin(hotSection, 8);
        TextView hotLabel = text("热门城市 (点击直达或查看辖区):", 11, MUTED, false);
        hotSection.addView(hotLabel);

        HorizontalScrollView hotScroll = new HorizontalScrollView(this);
        hotScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout hotRow = row();
        hotRow.setPadding(0, dp(4), 0, dp(4));

        String[] hotCities = {"北京", "上海", "武汉", "广州", "深圳", "杭州", "南京", "成都", "重庆", "西安"};
        for (String hotName : hotCities) {
            TextView chip = text(hotName, 12, INK, false);
            chip.setPadding(dp(12), dp(5), dp(12), dp(5));
            chip.setBackground(borderedBg(0xFFF1F5F9, BORDER, 14));
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                search.setText(hotName);
                search.setSelection(hotName.length());
            });
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-2, -2);
            chipLp.rightMargin = dp(6);
            hotRow.addView(chip, chipLp);
        }
        hotScroll.addView(hotRow);
        hotSection.addView(hotScroll);
        body.addView(hotSection);

        // Results ListView
        ListView list = new ListView(this);
        list.setDivider(new ColorDrawable(0xFFF1F5F9));
        list.setDividerHeight(dp(1));
        margin(list, 10);
        body.addView(list, new LinearLayout.LayoutParams(-1, dp(260)));

        TextView empty = text("未找到匹配城市或区县", 13, MUTED, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dp(40), 0, dp(40));
        body.addView(empty);
        list.setEmptyView(empty);

        final List<CityCatalog.City> currentResults = new ArrayList<>(catalog.getMainCities());
        final boolean[] showMainOnly = {true};

        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return currentResults.size(); }
            @Override public CityCatalog.City getItem(int i) { return currentResults.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View view, ViewGroup parent) {
                LinearLayout rowLayout;
                if (view instanceof LinearLayout) {
                    rowLayout = (LinearLayout) view;
                } else {
                    rowLayout = vertical();
                    rowLayout.setPadding(dp(8), dp(10), dp(8), dp(10));
                    rowLayout.setBackground(selectable());

                    LinearLayout titleRow = row();
                    titleRow.setGravity(Gravity.CENTER_VERTICAL);
                    TextView name = text("", 15, INK, true);
                    name.setTag("name");
                    titleRow.addView(name, new LinearLayout.LayoutParams(0, -2, 1));

                    TextView badge = text("", 10, Color.WHITE, false);
                    badge.setTag("badge");
                    badge.setPadding(dp(6), dp(2), dp(6), dp(2));
                    titleRow.addView(badge);
                    rowLayout.addView(titleRow);

                    TextView detail = text("", 11, MUTED, false);
                    detail.setTag("detail");
                    margin(detail, 4);
                    rowLayout.addView(detail);
                }

                CityCatalog.City c = getItem(i);
                TextView nameView = rowLayout.findViewWithTag("name");
                TextView badgeView = rowLayout.findViewWithTag("badge");
                TextView detailView = rowLayout.findViewWithTag("detail");

                nameView.setText(c.name + (c.isMainCity ? "市" : (c.name.endsWith("区") || c.name.endsWith("县") ? "" : "区/县")));
                if (c.isMainCity) {
                    badgeView.setText("地级市");
                    badgeView.setTextColor(0xFF0D9488);
                    badgeView.setBackground(bg(0x260D9488, 8));
                    detailView.setText(c.province + " · 核心主城区  (包含下辖各区县)");
                } else {
                    badgeView.setText("下辖区县");
                    badgeView.setTextColor(0xFF2563EB);
                    badgeView.setBackground(bg(0x1F2563EB, 8));
                    detailView.setText(c.province + " · 隶属于 " + c.parentCity + "市 · 编码 " + c.code);
                }

                return rowLayout;
            }
        };
        list.setAdapter(adapter);

        Runnable refreshList = () -> {
            String q = search.getText().toString().trim();
            currentResults.clear();
            if (q.isEmpty()) {
                if (showMainOnly[0]) {
                    currentResults.addAll(catalog.getMainCities());
                } else {
                    currentResults.addAll(catalog.search(""));
                }
            } else {
                currentResults.addAll(catalog.search(q));
            }
            adapter.notifyDataSetChanged();
        };

        tabMain.setOnClickListener(v -> {
            showMainOnly[0] = true;
            tabMain.setTextColor(Color.WHITE);
            tabMain.setBackground(bg(BLUE, 14));
            tabAll.setTextColor(MUTED);
            tabAll.setBackground(borderedBg(0xFFF8FAFC, BORDER, 14));
            refreshList.run();
        });

        tabAll.setOnClickListener(v -> {
            showMainOnly[0] = false;
            tabAll.setTextColor(Color.WHITE);
            tabAll.setBackground(bg(BLUE, 14));
            tabMain.setTextColor(MUTED);
            tabMain.setBackground(borderedBg(0xFFF8FAFC, BORDER, 14));
            refreshList.run();
        });

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshList.run();
            }
            public void afterTextChanged(Editable e) {}
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            CityCatalog.City clicked = currentResults.get(position);

            // If user clicked a main city without search, and that city has sub-districts:
            List<CityCatalog.City> districts = catalog.getDistrictsOf(clicked.name);
            if (clicked.isMainCity && !districts.isEmpty() && search.getText().toString().trim().isEmpty()) {
                // Show prompt to choose main city weather OR pick one of its sub-districts!
                showDistrictPicker(clicked, districts, dialog);
            } else {
                city = clicked;
                dialog.dismiss();
                load();
            }
        });

        dialog.setContentView(body, new ViewGroup.LayoutParams(dp(330), -2));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        dialog.show();
    }

    /**
     * Sub-picker when a main city is selected:
     * Choose between全市天气 (Main City Weather) or one of its specific districts (e.g. 江夏区)!
     */
    private void showDistrictPicker(CityCatalog.City mainCity, List<CityCatalog.City> districts, Dialog parentDialog) {
        Dialog districtDialog = new Dialog(this);
        districtDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = vertical();
        box.setPadding(dp(20), dp(20), dp(20), dp(18));
        box.setBackground(borderedBg(Color.WHITE, BORDER, 24));

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(mainCity.name + "市 · 辖区选择", 17, INK, true);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = text("✕", 18, MUTED, false);
        close.setPadding(dp(6), dp(2), dp(6), dp(2));
        close.setOnClickListener(v -> districtDialog.dismiss());
        head.addView(close);
        box.addView(head);

        TextView tip = text("请选择查看全市天气，或具体市辖区天气：", 12, MUTED, false);
        margin(tip, 10);
        box.addView(tip);

        // 1. Choose Main City option
        LinearLayout mainOption = row();
        mainOption.setGravity(Gravity.CENTER_VERTICAL);
        mainOption.setPadding(dp(14), dp(12), dp(14), dp(12));
        mainOption.setBackground(borderedBg(0xFFF0FDF4, 0xFFBBF7D0, 14));
        mainOption.setClickable(true);
        mainOption.setOnClickListener(v -> {
            city = mainCity;
            districtDialog.dismiss();
            parentDialog.dismiss();
            load();
        });

        TextView mainText = text("🌟 " + mainCity.name + "全市天气 (主城区)", 14, 0xFF166534, true);
        mainOption.addView(mainText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView codeText = text("编码 " + mainCity.code, 11, 0xFF15803D, false);
        mainOption.addView(codeText);
        margin(mainOption, 12);
        box.addView(mainOption);

        // 2. Sub-districts list
        TextView subLabel = text("下辖区县 (独立站点气象数据):", 12, INK, true);
        margin(subLabel, 14);
        box.addView(subLabel);

        ListView subList = new ListView(this);
        subList.setDivider(new ColorDrawable(0xFFF1F5F9));
        subList.setDividerHeight(dp(1));
        margin(subList, 8);
        box.addView(subList, new LinearLayout.LayoutParams(-1, dp(200)));

        BaseAdapter subAdapter = new BaseAdapter() {
            @Override public int getCount() { return districts.size(); }
            @Override public CityCatalog.City getItem(int i) { return districts.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View view, ViewGroup parent) {
                LinearLayout rowLayout = vertical();
                rowLayout.setPadding(dp(10), dp(10), dp(10), dp(10));
                rowLayout.setBackground(selectable());

                LinearLayout r = row();
                r.setGravity(Gravity.CENTER_VERTICAL);
                CityCatalog.City d = getItem(i);
                TextView name = text("📍 " + d.name + (d.name.endsWith("区") || d.name.endsWith("县") ? "" : "区"), 14, INK, false);
                r.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
                TextView c = text(d.code, 11, MUTED, false);
                r.addView(c);
                rowLayout.addView(r);

                return rowLayout;
            }
        };
        subList.setAdapter(subAdapter);
        subList.setOnItemClickListener((parent, view, position, id) -> {
            city = districts.get(position);
            districtDialog.dismiss();
            parentDialog.dismiss();
            load();
        });

        districtDialog.setContentView(box, new ViewGroup.LayoutParams(dp(320), -2));
        if (districtDialog.getWindow() != null) {
            districtDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        districtDialog.show();
    }

    private void showEmpty(String title, String message, boolean retry) {
        content.removeAllViews();
        LinearLayout empty = vertical();
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(30), dp(60), dp(30), dp(60));

        empty.addView(new WeatherIconView(this, "多云"), new LinearLayout.LayoutParams(dp(120), dp(120)));

        TextView heading = text(title, 20, INK, true);
        margin(heading, 20);
        empty.addView(heading);

        TextView detail = text(message, 13, MUTED, false);
        detail.setGravity(Gravity.CENTER);
        detail.setLineSpacing(dp(6), 1);
        margin(detail, 12);
        empty.addView(detail);

        if (retry) {
            Button button = new Button(this);
            button.setText("重新获取天气");
            button.setTextColor(Color.WHITE);
            button.setBackground(bg(BLUE, 18));
            button.setPadding(dp(20), dp(10), dp(20), dp(10));
            button.setOnClickListener(v -> load());
            margin(button, 24);
            empty.addView(button);
        }
        content.addView(empty);
    }

    private LinearLayout metricBox(String label, String value, String desc) {
        LinearLayout item = vertical();
        item.setPadding(dp(14), dp(12), dp(14), dp(12));
        item.setBackground(borderedBg(0xFFF8FAFC, BORDER, 16));

        TextView lbl = text(label, 11, MUTED, false);
        item.addView(lbl);

        TextView num = text(value, 18, INK, true);
        margin(num, 4);
        item.addView(num);

        TextView d = text(desc, 10, 0xFF94A3B8, false);
        margin(d, 4);
        item.addView(d);

        return item;
    }

    private void addHeading(LinearLayout parent, String title, String subtitle) {
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text(title, 16, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text(subtitle, 11, MUTED, false));
        parent.addView(heading);
    }

    private LinearLayout card() {
        LinearLayout panel = vertical();
        panel.setPadding(dp(18), dp(20), dp(18), dp(20));
        panel.setBackground(borderedBg(Color.WHITE, BORDER, 22));
        return panel;
    }

    private void addCard(View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.leftMargin = dp(16);
        params.rightMargin = dp(16);
        params.bottomMargin = dp(14);
        content.addView(view, params);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private GradientDrawable borderedBg(int bgColor, int borderColor, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(bgColor);
        background.setStroke(dp(1), borderColor);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private GradientDrawable gradientCard(int topColor, int bottomColor, int radius) {
        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{topColor, bottomColor}
        );
        background.setStroke(dp(1), BORDER);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private android.graphics.drawable.Drawable selectable() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return getDrawable(value.resourceId);
    }

    private void margin(View view, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        view.setLayoutParams(params);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String time(long millis) {
        if (millis <= 0) return "未保存";
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return format.format(new Date(millis));
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override protected void onDestroy() {
        destroyed = true;
        generation++;
        repository.cancel();
        worker.shutdownNow();
        super.onDestroy();
    }
}
