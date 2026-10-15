package com.example.toutiao.demo;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class CartOptionsTest {
    @Test public void longSkuLabelsUseReadableRows() {
        Context context = ApplicationProvider.getApplicationContext();
        ShopItem product = new ShopItem("sku-layout-test", "收纳箱", 45.9, 0, R.drawable.ic_shop, "家具", "",
                "{\"skuOptions\":[\"颜色：里瑟米 · 规格：特大号\",\"颜色：里瑟米 · 规格：大号\"]}");
        try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, product)
                .putExtra(ProductDetailActivity.EXTRA_OPEN_OPTIONS, true))) {
            onView(withId(R.id.ll_sku_specs)).check((view, error) -> {
                if (error != null) throw error;
                android.widget.LinearLayout options = (android.widget.LinearLayout)view;
                assertEquals(android.widget.LinearLayout.VERTICAL, options.getOrientation());
                assertTrue(options.getChildAt(0).getWidth() >= options.getWidth() * .9);
                assertTrue(options.getChildAt(1).getWidth() >= options.getWidth() * .9);
                assertTrue(options.getChildAt(1).getTop() >= options.getChildAt(0).getBottom());
            });
        }
    }
    @Test public void changeSkuAndQuantityEditsExistingRow() {
        Context context = ApplicationProvider.getApplicationContext();
        ShopStore store = new ShopStore(context);
        java.util.List<ShopStore.CartItem> previous = store.getCartItems();
        ShopStore.CartItem original = new ShopStore.CartItem("cart-edit-options", "颜色：白色", "商品", 1000, 2, 0, "家具", false);
        try {
            store.saveCart(java.util.Collections.singletonList(original));
            ShopItem product = new ShopItem(original.productId, original.title, 10, 0, R.drawable.ic_shop, "家具", "",
                    "{\"source\":\"在线商品\",\"skuOptions\":[\"颜色：白色\",\"颜色：黑色\"],\"skuOptionPrices\":{\"颜色：黑色\":12.34}}");
            android.content.Intent intent = ProductDetailActivity.intent(context, product)
                    .putExtra(ProductDetailActivity.EXTRA_CART_KEY, original.key())
                    .putExtra(ProductDetailActivity.EXTRA_OPEN_OPTIONS, true);
            try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(intent)) {
                onView(withId(R.id.tv_sku_qty)).check(matches(withText("2")));
                onView(withText("颜色：黑色")).perform(click());
                onView(withId(R.id.btn_sku_plus)).perform(click());
                onView(withId(R.id.tv_sku_price)).check(matches(withText("¥12.34")));
                onView(withId(R.id.tv_sku_confirm)).check(matches(withText("确认修改"))).perform(click());
                assertEquals(1, store.getCartItems().size());
                ShopStore.CartItem saved = store.getCartItems().get(0);
                assertEquals("颜色：黑色", saved.sku);
                assertEquals(3, saved.quantity);
                assertEquals(1234, saved.priceCents);
                assertFalse(saved.selected);
            }
        } finally { store.saveCart(previous); }
    }
}
