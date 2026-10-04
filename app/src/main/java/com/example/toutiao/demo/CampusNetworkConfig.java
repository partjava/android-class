package com.example.toutiao.demo;

import android.content.Context;
import java.net.URI;

/** One persisted service origin for both emulators and physical devices. */
public final class CampusNetworkConfig {
    public static final String EMULATOR_URL = "http://10.0.2.2:5000";
    private CampusNetworkConfig() { }
    public static void initialize(Context context) {
        CampusNewsStore.setServerUrl(context.getSharedPreferences("campus_network", Context.MODE_PRIVATE)
                .getString("server_url", EMULATOR_URL));
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
            throw new IllegalArgumentException("请输入完整服务地址，如 http://192.168.1.8:5000");
        }
    }
    public static void save(Context context, String url) {
        String normalized = normalize(url);
        context.getSharedPreferences("campus_network", Context.MODE_PRIVATE).edit()
                .putString("server_url", normalized).apply();
        CampusNewsStore.setServerUrl(normalized);
    }
}
