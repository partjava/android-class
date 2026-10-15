package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.ImageView;
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
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CartDisplayTest {
    @Test public void restoredCartShowsSourceImageTitleAndExactCentsWithoutPostingAnotherAdd() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ShopStore store = new ShopStore(context);
        java.util.List<ShopStore.CartItem> previous = store.getCartItems();
        android.content.SharedPreferences prefs = context.getSharedPreferences("campus_network", 0);
        String oldUrl = prefs.getString("server_url", null), oldEngine = prefs.getString("engine_type", null);
        String oldStore = CampusNewsStore.getServerUrl();
        AtomicReference<String> error = new AtomicReference<>();
        try (ServerSocket server = new ServerSocket(0)) {
            store.saveCart(java.util.Collections.singletonList(new ShopStore.CartItem("cart-display-test", "默认规格", "错误旧名称", 1, 3, 0, "其他", true)));
            server.setSoTimeout(120000);
            String body = "{\"code\":200,\"data\":{\"items\":[{\"product_id\":\"cart-display-test\",\"title\":\"官网购物车商品\",\"price\":19.99,\"count\":3,\"category\":\"家具\",\"image_url\":\"http://127.0.0.1:1/product.jpg\"}]}}";
            Thread fixture = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String first = reader.readLine();
                    if (!first.startsWith("GET /api/v1/cart ")) throw new AssertionError(first);
                    String line; while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
                } catch (Throwable e) { error.set(e.toString()); }
            });
            fixture.start();
            CampusNetworkConfig.save(context, "http://127.0.0.1:" + server.getLocalPort(), CampusNetworkConfig.ENGINE_FASTAPI);
            try (ActivityScenario<CartActivity> scenario = ActivityScenario.launch(new Intent(context, CartActivity.class))) {
                AtomicReference<String> title = new AtomicReference<>("");
                for (int i = 0; i < 100; i++) {
                    scenario.onActivity(a -> {
                        RecyclerView rv = a.findViewById(R.id.rv_cart);
                        RecyclerView.ViewHolder row = rv.findViewHolderForAdapterPosition(0);
                        if (row != null) title.set(((TextView)row.itemView.findViewById(R.id.tv_title)).getText().toString());
                    });
                    if (title.get().equals("官网购物车商品")) break;
                    Thread.sleep(100);
                }
                assertEquals("官网购物车商品", title.get());
                scenario.onActivity(a -> {
                    RecyclerView.ViewHolder row = ((RecyclerView)a.findViewById(R.id.rv_cart)).findViewHolderForAdapterPosition(0);
                    assertEquals("¥19.99", ((TextView)row.itemView.findViewById(R.id.tv_price)).getText().toString());
                    assertEquals("http://127.0.0.1:1/product.jpg", ((ImageView)row.itemView.findViewById(R.id.iv_thumb)).getTag());
                    assertEquals("¥59.97", ((TextView)a.findViewById(R.id.tv_total_amount)).getText().toString());
                });
                assertEquals(1999, store.getCartItems().get(0).priceCents);
                server.setSoTimeout(1000);
                try (Socket extra = server.accept()) { fail("Restoring a cart must not send another add request"); }
                catch (java.net.SocketTimeoutException expected) { }
                assertNull(error.get());
                android.app.Instrumentation instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
                android.app.Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(ProductDetailActivity.class.getName(), null, true);
                try {
                    scenario.onActivity(a -> {
                        RecyclerView.ViewHolder row = ((RecyclerView)a.findViewById(R.id.rv_cart)).findViewHolderForAdapterPosition(0);
                        row.itemView.findViewById(R.id.iv_thumb).performClick();
                        row.itemView.findViewById(R.id.tv_category).performClick();
                    });
                    assertEquals("Image and SKU must both open product details", 2, monitor.getHits());
                } finally { instrumentation.removeMonitor(monitor); }
            }
        } finally {
            store.saveCart(previous);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            if (oldUrl == null) editor.remove("server_url"); else editor.putString("server_url", oldUrl);
            if (oldEngine == null) editor.remove("engine_type"); else editor.putString("engine_type", oldEngine);
            editor.commit(); CampusNetworkConfig.initialize(context); CampusNewsStore.setServerUrl(oldStore);
        }
    }
}
