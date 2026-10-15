package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OrderImageDisplayTest {
    private static final String IMAGE = "http://127.0.0.1:1/order-product.jpg";
    @Test public void cartCheckoutRetainsProductImage() {
        Context context = ApplicationProvider.getApplicationContext();
        ShopStore store = new ShopStore(context);
        java.util.List<ShopStore.CartItem> previous = store.getCartItems();
        try {
            ShopStore.CartItem item = new ShopStore.CartItem("order-image-test", "大号", "官网商品", 7990, 1, R.drawable.ic_shop, "家具", true);
            item.imageUrl = IMAGE;
            store.saveCart(java.util.Collections.singletonList(item));
            check(new Intent(context, OrderConfirmActivity.class));
        } finally { store.saveCart(previous); }
    }
    @Test public void directCheckoutRetainsProductImage() {
        Context context = ApplicationProvider.getApplicationContext();
        check(new Intent(context, OrderConfirmActivity.class)
                .putExtra(OrderConfirmActivity.EXTRA_DIRECT_ID, "order-direct-image")
                .putExtra(OrderConfirmActivity.EXTRA_DIRECT_TITLE, "官网商品")
                .putExtra(OrderConfirmActivity.EXTRA_DIRECT_PRICE_CENTS, 7990L)
                .putExtra(OrderConfirmActivity.EXTRA_DIRECT_IMG, R.drawable.ic_shop)
                .putExtra("extra_direct_image_url", IMAGE));
    }
    private void check(Intent intent) {
        try (ActivityScenario<OrderConfirmActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                LinearLayout products = activity.findViewById(R.id.ll_order_products);
                assertEquals(1, products.getChildCount());
                ImageView image = products.getChildAt(0).findViewById(R.id.iv_thumb);
                assertEquals(IMAGE, image.getTag());
            });
        }
    }
}
