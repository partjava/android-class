package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ShopDataSourceTest {
    @Test public void emptyCatalogNeverShowsLocalSamples() throws Exception { check("200 OK", "{\"code\":200,\"data\":{\"items\":[]}}", 0, "暂无商品"); }
    @Test public void failedRequestNeverShowsLocalSamples() throws Exception { check("503 Unavailable", "{}", 0, "商品加载失败"); }
    @Test public void onlyReturnedProductsAreDisplayed() throws Exception { check("200 OK", "{\"code\":200,\"data\":{\"items\":[{\"product_id\":\"test-only\",\"title\":\"测试专用商品\",\"price\":12.34,\"category\":\"其他\",\"sales\":0}]}}", 1, "已加载"); }

    private void check(String status, String body, int count, String expected) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String old = CampusNewsStore.getServerUrl();
        android.content.SharedPreferences prefs = context.getSharedPreferences("campus_network", 0);
        String oldUrl = prefs.getString("server_url", null), oldEngine = prefs.getString("engine_type", null);
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> serverError = new AtomicReference<>();
        try (ServerSocket server = new ServerSocket(0)) {
            server.setSoTimeout(120000);
            Thread fixture = new Thread(() -> {
                try {
                  for (int request = 0; request < (count == 1 ? 3 : 1); request++) try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String first = reader.readLine();
                    if (request == 1 && (!first.contains("page=2") || first.contains("refresh=true"))) throw new AssertionError(first);
                    if (request == 2 && (!first.contains("page=1") || !first.contains("refresh=true"))) throw new AssertionError(first);
                    requests.incrementAndGet();
                    String line; while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                    String responseBody = body;
                    if (count == 1) {
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        org.json.JSONObject data = json.getJSONObject("data");
                        data.put("has_more", request != 1);
                        if (request == 1) {
                            org.json.JSONObject extra = new org.json.JSONObject(data.getJSONArray("items").getJSONObject(0).toString());
                            extra.put("product_id", "test-extra"); extra.put("title", "下一页商品");
                            data.getJSONArray("items").put(extra);
                        }
                        responseBody = json.toString();
                    }
                    byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.1 " + status + "\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
                  }
                } catch (Throwable failure) { serverError.set(failure.toString()); }
            });
            fixture.start();
            CampusNetworkConfig.save(context, "http://127.0.0.1:" + server.getLocalPort(), CampusNetworkConfig.ENGINE_FASTAPI);
            try (ActivityScenario<ShopActivity> scenario = ActivityScenario.launch(new Intent(context, ShopActivity.class))) {
                AtomicReference<String> text = new AtomicReference<>("");
                AtomicInteger rows = new AtomicInteger(-1);
                for (int i = 0; i < 70; i++) {
                    scenario.onActivity(activity -> {
                        text.set(((TextView)activity.findViewById(R.id.tv_goods_status)).getText().toString());
                        rows.set(((RecyclerView)activity.findViewById(R.id.rv_shop)).getAdapter().getItemCount());
                    });
                    if (text.get().contains(expected)) break;
                    Thread.sleep(100);
                }
                assertTrue(text.get(), text.get().contains(expected));
                assertEquals(count, rows.get());
                if (count == 1) {
                    android.app.Instrumentation instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
                    android.app.Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(WebActivity.class.getName(), null, false);
                    try {
                        scenario.onActivity(activity -> activity.findViewById(R.id.tv_shop_footer).performClick());
                        assertNull("Footer must refresh goods instead of opening coupon page", instrumentation.waitForMonitorWithTimeout(monitor, 1000));
                        for (int i = 0; i < 70 && requests.get() < 2; i++) Thread.sleep(100);
                        assertNull(serverError.get());
                        assertEquals("Footer must request next page", 2, requests.get());
                        for (int i = 0; i < 70; i++) {
                            scenario.onActivity(a -> rows.set(((RecyclerView)a.findViewById(R.id.rv_shop)).getAdapter().getItemCount()));
                            if (rows.get() == 2) break;
                            Thread.sleep(100);
                        }
                        assertEquals("Append must keep first item and deduplicate repeated IDs", 2, rows.get());
                        scenario.onActivity(a -> {
                            assertFalse(a.findViewById(R.id.tv_shop_footer).isEnabled());
                            ((androidx.core.widget.NestedScrollView)a.findViewById(R.id.nsv_shop)).scrollTo(0, 0);
                            android.view.View pull = a.findViewById(R.id.pull_shop);
                            long t = android.os.SystemClock.uptimeMillis();
                            for (int action : new int[]{0, 2, 2, 1}) {
                                android.view.MotionEvent event = android.view.MotionEvent.obtain(t, t + 100, action, 80, action == 0 ? 100 : 600, 0);
                                pull.dispatchTouchEvent(event); event.recycle();
                            }
                        });
                        for (int i = 0; i < 70; i++) {
                            scenario.onActivity(a -> rows.set(((RecyclerView)a.findViewById(R.id.rv_shop)).getAdapter().getItemCount()));
                            if (requests.get() == 3 && rows.get() == 1) break;
                            Thread.sleep(100);
                        }
                        assertNull(serverError.get());
                        assertEquals("Pull refresh must reload first page", 3, requests.get());
                        assertEquals("Refresh must replace appended pages", 1, rows.get());
                    } finally { instrumentation.removeMonitor(monitor); }
                }
            }
            fixture.join(1000);
        } finally {
            android.content.SharedPreferences.Editor editor = prefs.edit();
            if (oldUrl == null) editor.remove("server_url"); else editor.putString("server_url", oldUrl);
            if (oldEngine == null) editor.remove("engine_type"); else editor.putString("engine_type", oldEngine);
            editor.commit(); CampusNetworkConfig.initialize(context); CampusNewsStore.setServerUrl(old);
        }
    }
}
