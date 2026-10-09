package com.example.toutiao.demo;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Only bundled shop pages may receive the native bridge. */
final class WebPagePolicy {
    private static final Set<String> PAGES = new HashSet<>(Arrays.asList(
            "about", "auction", "categories", "coupons", "farm", "feizhu", "flashsale",
            "gifts", "global", "help", "live", "newproducts", "newuser", "pharmacy",
            "privacy", "recharge", "rules", "seckill", "security", "signin", "subsidy",
            "supermarket", "temai", "vip", "xianyu"));

    static boolean isTrustedPage(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            String path = uri.getRawPath();
            if (!"file".equals(uri.getScheme()) || uri.getRawAuthority() != null || uri.getRawQuery() != null
                    || path == null || !path.startsWith("/android_asset/web/") || !path.endsWith(".html")) return false;
            return PAGES.contains(path.substring("/android_asset/web/".length(), path.length() - 5));
        } catch (java.net.URISyntaxException e) { return false; }
    }
}
