package com.example.toutiao.demo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Compatibility keys for old records that did not carry an explicit identifier. */
public final class LegacyIds {
    private LegacyIds() {}
    public static String news(String title) { return id("legacy-news-", title); }
    public static String product(String title) { return id("legacy-product-", title); }
    private static String id(String prefix, String value) {
        return prefix + UUID.nameUUIDFromBytes((prefix + (value == null ? "" : value)).getBytes(StandardCharsets.UTF_8));
    }
}
