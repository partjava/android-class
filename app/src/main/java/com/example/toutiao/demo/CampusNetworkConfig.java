package com.example.toutiao.demo;

import android.content.Context;
import java.net.URI;

/** One persisted service origin for both emulators and physical devices, supporting Flask & FastAPI engines. */
public final class CampusNetworkConfig {
    public static final String EMULATOR_URL = "http://10.0.2.2:8000";
    public static final String FLASK_EMULATOR_URL = "http://10.0.2.2:5000";
    public static final String FASTAPI_EMULATOR_URL = "http://10.0.2.2:8000";

    public static final String ENGINE_FLASK = "flask";
    public static final String ENGINE_FASTAPI = "fastapi";

    private static String currentEngine = ENGINE_FASTAPI;

    private CampusNetworkConfig() { }

    public static void initialize(Context context) {
        currentEngine = context.getSharedPreferences("campus_network", Context.MODE_PRIVATE)
                .getString("engine_type", ENGINE_FASTAPI);
        String defaultUrl = ENGINE_FASTAPI.equals(currentEngine) ? FASTAPI_EMULATOR_URL : FLASK_EMULATOR_URL;
        CampusNewsStore.setServerUrl(context.getSharedPreferences("campus_network", Context.MODE_PRIVATE)
                .getString("server_url", defaultUrl));
    }

    public static String getEngine(Context context) {
        if (context == null) return currentEngine;
        return context.getSharedPreferences("campus_network", Context.MODE_PRIVATE)
                .getString("engine_type", currentEngine);
    }

    public static String getApiBaseUrl() {
        String base = CampusNewsStore.getServerUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/api/v1";
    }

    public static String normalize(String value) {
        String text = value.trim();
        if (!text.contains("://")) text = "http://" + text;
        try {
            URI uri = new URI(text);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))
                    || uri.getPort() > 65535 || uri.getPort() == 0) throw new Exception();
            return new URI(scheme.toLowerCase(java.util.Locale.ROOT), null, uri.getHost(),
                    uri.getPort(), null, null, null).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("请输入完整服务地址，如 http://192.168.1.8:8000");
        }
    }

    public static void save(Context context, String url) {
        save(context, url, currentEngine);
    }

    public static void save(Context context, String url, String engine) {
        String normalized = normalize(url);
        currentEngine = engine;
        context.getSharedPreferences("campus_network", Context.MODE_PRIVATE).edit()
                .putString("server_url", normalized)
                .putString("engine_type", engine)
                .apply();
        CampusNewsStore.setServerUrl(normalized);
    }
}
