package com.example.toutiao.demo;

import android.content.Context;
import android.widget.TextView;
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
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProductDetailLoadingTest {
    @Test public void fetchByIdThenRefreshFailurePreservesDetail() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        android.content.SharedPreferences prefs = context.getSharedPreferences("campus_network", 0);
        String oldUrl = prefs.getString("server_url", null), oldEngine = prefs.getString("engine_type", null);
        String oldStore = CampusNewsStore.getServerUrl();
        AtomicReference<String> error = new AtomicReference<>();
        try (ServerSocket server = new ServerSocket(0)) {
            server.setSoTimeout(120000);
            Thread fixture = new Thread(() -> {
                try {
                    for (int i = 0; i < 2; i++) try (Socket socket = server.accept()) {
                        socket.setSoTimeout(5000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        String first = reader.readLine();
                        if (!first.contains("/goods/yanxuan-4064594?refresh=" + (i == 1))) throw new AssertionError(first);
                        String line; while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                        String body = i == 0 ? "{\"code\":200,\"data\":{\"product_id\":\"yanxuan-4064594\",\"title\":\"真实详情标题\",\"price\":129,\"details\":{\"source\":\"网易严选\",\"commentCount\":12,\"commentGoodRates\":\"99%\",\"reviewsStatus\":\"loaded\",\"reviews\":[{\"reviewerName\":\"用户\",\"comment\":\"真实评价正文\",\"rating\":5}]}}}" : "{}";
                        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                        socket.getOutputStream().write(("HTTP/1.1 " + (i == 0 ? "200 OK" : "503 Unavailable") + "\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
                    }
                } catch (Throwable e) { error.set(e.toString()); }
            });
            fixture.start();
            CampusNetworkConfig.save(context, "http://127.0.0.1:" + server.getLocalPort(), CampusNetworkConfig.ENGINE_FASTAPI);
            ShopItem summary = new ShopItem("yanxuan-4064594", "摘要标题", 129, 0, R.drawable.ic_shop, "推荐", "", "{\"summaryOnly\":true}");
            try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, summary))) {
                AtomicReference<String> title = new AtomicReference<>("");
                for (int i = 0; i < 100; i++) {
                    scenario.onActivity(a -> title.set(((TextView)a.findViewById(R.id.tv_detail_title)).getText().toString()));
                    if ("真实详情标题".equals(title.get())) break;
                    Thread.sleep(100);
                }
                assertEquals("真实详情标题", title.get());
                scenario.onActivity(a -> {
                    assertEquals("商品评价 (12)", ((TextView)a.findViewById(R.id.tv_review_header)).getText().toString());
                    assertEquals("好评率 99%", ((TextView)a.findViewById(R.id.tv_review_rate)).getText().toString());
                    a.findViewById(R.id.btn_gallery_more).performClick();
                });
                fixture.join(10000);
                assertNull(error.get());
                Thread.sleep(500);
                scenario.onActivity(a -> assertEquals("真实详情标题", ((TextView)a.findViewById(R.id.tv_detail_title)).getText().toString()));
            }
        } finally {
            android.content.SharedPreferences.Editor editor = prefs.edit();
            if (oldUrl == null) editor.remove("server_url"); else editor.putString("server_url", oldUrl);
            if (oldEngine == null) editor.remove("engine_type"); else editor.putString("engine_type", oldEngine);
            editor.commit(); CampusNetworkConfig.initialize(context); CampusNewsStore.setServerUrl(oldStore);
        }
    }
}
