package com.example.toutiao.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class WebPagePolicyTest {
    @Test public void onlyPackagedPagesAreTrusted() {
        assertTrue(WebPagePolicy.isTrustedPage("file:///android_asset/web/signin.html"));
        assertTrue(WebPagePolicy.isTrustedPage("file:///android_asset/web/coupons.html#claimed"));
        for (String url : new String[]{null, "https://example.com/signin.html", "file:///sdcard/signin.html",
                "file:///android_asset/web/../signin.html", "file:///android_asset/web/%2e%2e/signin.html",
                "file:///android_asset/web/unknown.html", "file://evil/android_asset/web/signin.html",
                "data:text/html,<script>Android.addCoins(999)</script>"}) {
            assertFalse(String.valueOf(url), WebPagePolicy.isTrustedPage(url));
        }
    }
}
