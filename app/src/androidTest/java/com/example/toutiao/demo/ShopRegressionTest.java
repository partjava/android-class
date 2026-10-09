package com.example.toutiao.demo;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.WebView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ShopRegressionTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test public void signInPersistsOncePerDayAcrossStoreInstances() {
        String name = "signin-test-" + System.nanoTime();
        try {
            ProfileStore store = new ProfileStore(context, name);
            int before = store.getCoins();
            LocalDate day = LocalDate.of(2026, 10, 9);
            assertTrue(store.signIn(day));
            assertFalse(new ProfileStore(context, name).signIn(day));
            assertEquals(before + 50, store.getCoins());
            assertTrue(new ProfileStore(context, name).signIn(day.plusDays(1)));
            assertEquals(before + 100, store.getCoins());
        } finally { context.getSharedPreferences(name, 0).edit().clear().commit(); }
    }

    @Test public void orderNoteSurvivesReloadAndLegacyOrders() {
        ShopStore store = new ShopStore(context);
        List<ShopStore.CartItem> cart = store.getCartItems();
        List<ShopStore.OrderItem> orders = store.getOrders();
        try {
            store.addToCart("备注测试", 1000, R.drawable.shop_1, "测试", 1);
            ShopStore.OrderItem order = store.createOrder(store.getCartItems(), "测试", "13800138000", "校园", 0, false, " 请勿折叠🌟 ");
            assertEquals("请勿折叠🌟", new ShopStore(context).getOrder(order.orderId).buyerNote);
            org.json.JSONObject legacy = order.toJson(); legacy.remove("buyerNote");
            assertEquals("", ShopStore.OrderItem.fromJson(legacy).buyerNote);
        } finally { store.saveCart(cart); store.saveOrders(orders); }
    }

    @Test public void checkoutRefreshesNewCouponWhenReturning() {
        ShopStore store = new ShopStore(context);
        SharedPreferences prefs = context.getSharedPreferences("shop_store_v2", 0);
        Map<String, ?> values = prefs.getAll();
        Intent intent = new Intent(context, OrderConfirmActivity.class)
                .putExtra(OrderConfirmActivity.EXTRA_DIRECT_TITLE, "回归商品")
                .putExtra(OrderConfirmActivity.EXTRA_DIRECT_PRICE_CENTS, 10000L);
        try {
            prefs.edit().putString("user_coupons_v1", "[]").commit();
            try (ActivityScenario<OrderConfirmActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
                assertTrue(store.addCoupon(10, "新领取的测试券", 0, "测试"));
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
                scenario.onActivity(activity -> ((android.view.View)activity.findViewById(R.id.ll_coupon_selector)).performClick());
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(
                        org.hamcrest.Matchers.containsString("新领取的测试券")))
                        .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            }
        } finally { restore(prefs, values); }
    }

    @Test public void checkoutNoteAppearsInOrderDetails() {
        ShopStore store = new ShopStore(context);
        List<ShopStore.OrderItem> orders = store.getOrders();
        List<ShopStore.CartItem> cart = store.getCartItems();
        SharedPreferences profile = context.getSharedPreferences("profile", 0);
        SharedPreferences shop = context.getSharedPreferences("shop_store_v2", 0);
        Map<String, ?> profileValues = profile.getAll(), shopValues = shop.getAll();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(OrderDetailActivity.class.getName(), null, false);
        android.app.Activity detail = null;
        try {
            profile.edit().putInt("user_coins", 0).commit();
            shop.edit().putString("user_coupons_v1", "[]").commit();
            Intent intent = new Intent(context, OrderConfirmActivity.class)
                    .putExtra(OrderConfirmActivity.EXTRA_DIRECT_TITLE, "备注回归商品")
                    .putExtra(OrderConfirmActivity.EXTRA_DIRECT_PRICE_CENTS, 10000L);
            try (ActivityScenario<OrderConfirmActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> {
                    ((android.widget.EditText)activity.findViewById(R.id.et_buyer_note)).setText("请勿折叠🌟");
                    activity.findViewById(R.id.btn_submit_order).performClick();
                });
                detail = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
                assertNotNull(detail);
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.tv_buyer_note))
                        .check(androidx.test.espresso.assertion.ViewAssertions.matches(
                                androidx.test.espresso.matcher.ViewMatchers.withText("订单备注: 请勿折叠🌟")));
                assertEquals("请勿折叠🌟", new ShopStore(context).getOrders().get(0).buyerNote);
            }
        } finally {
            if (detail != null) { android.app.Activity activity = detail; instrumentation.runOnMainSync(activity::finish); }
            instrumentation.removeMonitor(monitor);
            store.saveCart(cart); store.saveOrders(orders);
            restore(profile, profileValues); restore(shop, shopValues);
        }
    }

    @Test public void webActivityIsPrivateAndTrustedSignInHasNoArbitraryCoinApi() throws Exception {
        assertFalse(context.getPackageManager().getActivityInfo(new android.content.ComponentName(context, WebActivity.class), 0).exported);
        SharedPreferences prefs = context.getSharedPreferences("profile", 0);
        Map<String, ?> values = prefs.getAll();
        try {
            prefs.edit().remove("last_signin_date").putInt("user_coins", 100).commit();
            Intent intent = new Intent(context, WebActivity.class).putExtra(WebActivity.EXTRA_URL, "file:///android_asset/web/signin.html");
            try (ActivityScenario<WebActivity> scenario = ActivityScenario.launch(intent)) {
                waitForBridge(scenario);
                assertEquals("\"function\"", evaluate(scenario, "typeof doSignin"));
                assertEquals("\"undefined\"", evaluate(scenario, "typeof Android.addCoins"));
                evaluate(scenario, "var evil=document.createElement('iframe');evil.src='data:text/html,'+encodeURIComponent('<script>Android.signIn()</script>');document.body.appendChild(evil)");
                Thread.sleep(300);
                assertEquals(100, new ProfileStore(context).getCoins());
                evaluate(scenario, "document.querySelector('[onclick=\"doSignin(this)\"]').click()");
                assertEquals("false", evaluate(scenario, "Android.signIn()"));
                assertEquals(150, new ProfileStore(context).getCoins());
                scenario.onActivity(activity -> ((WebView)activity.findViewById(R.id.wv_content)).reload());
                waitForBridge(scenario);
                assertEquals("true", evaluate(scenario, "Android.hasSignedInToday()"));
                assertEquals("\"今日已签到\"", evaluate(scenario, "document.querySelector('[onclick=\"doSignin(this)\"]').innerText"));
                assertEquals("true", evaluate(scenario, "document.styleSheets.length > 0"));
            }
        } finally { restore(prefs, values); }
    }

    private void waitForBridge(ActivityScenario<WebActivity> scenario) throws Exception {
        for (int i = 0; i < 80; i++) {
            if ("true".equals(evaluate(scenario, "typeof Android === 'object' && typeof doSignin === 'function' && document.readyState !== 'loading'"))) return;
            Thread.sleep(100);
        }
        fail("Packaged page bridge did not load");
    }
    private String evaluate(ActivityScenario<WebActivity> scenario, String script) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        scenario.onActivity(activity -> ((WebView)activity.findViewById(R.id.wv_content)).evaluateJavascript(script, value -> { result.set(value); latch.countDown(); }));
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return result.get();
    }
    private static void restore(SharedPreferences prefs, Map<String, ?> values) {
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object v = entry.getValue(); String k = entry.getKey();
            if (v instanceof String) editor.putString(k, (String)v);
            else if (v instanceof Integer) editor.putInt(k, (Integer)v);
            else if (v instanceof Boolean) editor.putBoolean(k, (Boolean)v);
            else if (v instanceof Long) editor.putLong(k, (Long)v);
            else if (v instanceof Float) editor.putFloat(k, (Float)v);
        }
        editor.commit();
    }
}
