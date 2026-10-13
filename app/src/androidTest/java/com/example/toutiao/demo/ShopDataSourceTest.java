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
        try (ServerSocket server = new ServerSocket(0)) {
            server.setSoTimeout(120000);
            Thread fixture = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String line; while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.1 " + status + "\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
                } catch (Exception ignored) { }
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
