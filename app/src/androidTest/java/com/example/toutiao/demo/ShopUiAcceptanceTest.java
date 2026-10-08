package com.example.toutiao.demo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** End-to-end UI evidence; always restores the user's rows and preferences. */
@RunWith(AndroidJUnit4.class)
public class ShopUiAcceptanceTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = ApplicationProvider.getApplicationContext();
    private final List<Activity> activities = new ArrayList<>();

    @Test public void productSkuCartAndSimulatedPayment() throws Exception {
        ShopStore store = new ShopStore(context);
        List<ShopStore.CartItem> originalCart = store.getCartItems();
        List<ShopStore.OrderItem> originalOrders = store.getOrders();
        SharedPreferences shop = context.getSharedPreferences("shop_store_v2", Context.MODE_PRIVATE);
        SharedPreferences profile = context.getSharedPreferences("profile", Context.MODE_PRIVATE);
        Map<String, ?> shopValues = shop.getAll();
        Map<String, ?> profileValues = profile.getAll();
        Instrumentation.ActivityMonitor confirmMonitor = null;
        Instrumentation.ActivityMonitor detailMonitor = null;
        try {
            store.clearCart();
            store.saveOrders(new ArrayList<>());
            profile.edit().putInt("user_coins", 0).commit();
            List<ShopStore.CouponItem> coupons = new ArrayList<>();
            coupons.add(new ShopStore.CouponItem("ui-test-coupon", "验收立减10元", 10, 0, "本地演示优惠券", false, 1));
            store.saveCoupons(coupons);
            ShopItem earphones = new ShopItem("无线蓝牙耳机 半入耳式 超长续航 通话降噪", 129, 0, R.drawable.shop_1, "数码");
            ShopItem cup = new ShopItem("316不锈钢保温杯 大容量便携车载水杯", 59.9, 0, R.drawable.shop_2, "家居");
            Activity product = openProduct(earphones);
            instrumentation.runOnMainSync(() -> {
                TextView description = product.findViewById(R.id.tv_desc_body1);
                assertTrue(description.getText() instanceof Spanned);
                assertTrue(((Spanned) description.getText()).getSpans(0, description.length(), android.text.style.StyleSpan.class).length > 0);
                assertTrue(description.getText().toString().contains("商品亮点"));
                NestedScrollView scroll = product.findViewById(R.id.nsv_detail);
                View card = product.findViewById(R.id.ll_desc_card);
                scroll.scrollTo(0, card.getTop());
            });
            instrumentation.waitForIdleSync();
            screenshot("product-detail.png");
            onView(withId(R.id.btn_add_cart)).perform(click());
            onView(withId(R.id.ll_sku_specs)).perform(secondSpecification());
            onView(withId(R.id.tv_sku_confirm)).perform(click());
            assertEquals(1, store.getCartItems().size());
            assertNotEquals("默认规格", store.getCartItems().get(0).sku);
            instrumentation.runOnMainSync(product::finish);
            openProduct(cup);
            onView(withId(R.id.btn_add_cart)).perform(click());
            onView(withId(R.id.tv_sku_confirm)).perform(click());
            assertEquals(2, store.getCartItems().size());
            instrumentation.runOnMainSync(activities.get(activities.size() - 1)::finish);
            openActivity(CartActivity.class);
            onView(withId(R.id.rv_cart)).perform(rowAction(earphones.getTitle(), R.id.btn_plus, click()));
            onView(withId(R.id.tv_total_amount)).check(matches(withText("¥317.90")));
            onView(withId(R.id.rv_cart)).perform(rowAction(cup.getTitle(), R.id.cb_select, click()));
            onView(withId(R.id.tv_total_amount)).check(matches(withText("¥258.00")));
            screenshot("cart-selected.png");
            onView(withId(R.id.rv_cart)).perform(rowAction(cup.getTitle(), 0, longClick()));
            onView(withText("移出购物车")).perform(click());
            assertEquals(1, store.getCartItems().size());
            confirmMonitor = instrumentation.addMonitor(OrderConfirmActivity.class.getName(), null, false);
            onView(withId(R.id.btn_checkout)).perform(click());
            Activity confirmation = instrumentation.waitForMonitorWithTimeout(confirmMonitor, 5000);
            assertNotNull(confirmation); activities.add(confirmation);
            onView(withId(R.id.tv_pay_amount)).check(matches(withText("¥248.00")));
            onView(withId(R.id.btn_submit_order)).check(matches(withText("模拟支付并创建订单")));
            detailMonitor = instrumentation.addMonitor(OrderDetailActivity.class.getName(), null, false);
            onView(withId(R.id.btn_submit_order)).perform(click());
            Activity result = instrumentation.waitForMonitorWithTimeout(detailMonitor, 5000);
            assertNotNull(result); activities.add(result);
            onView(withId(R.id.tv_detail_status_desc)).check(matches(withText("本地模拟支付成功 · 订单已保存（不涉及真实资金）")));
            assertEquals(0, store.getCartItems().size());
            ShopStore.OrderItem order = new ShopStore(context).getOrders().get(0);
            assertEquals(25800, order.totalCents);
            assertEquals(1000, order.discountCents);
            assertEquals(24800, order.actualCents);
            assertEquals(2, order.items.get(0).quantity);
            assertNotEquals("默认规格", order.items.get(0).sku);
            screenshot("order-result.png");
        } finally {
            if (confirmMonitor != null) instrumentation.removeMonitor(confirmMonitor);
            if (detailMonitor != null) instrumentation.removeMonitor(detailMonitor);
            instrumentation.runOnMainSync(() -> { for (int i = activities.size() - 1; i >= 0; i--) activities.get(i).finish(); });
            new DemoDatabase(context).transaction(() -> { store.saveCart(originalCart); store.saveOrders(originalOrders); return null; });
            restore(shop, shopValues); restore(profile, profileValues);
        }
    }

    private Activity openProduct(ShopItem item) {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(ProductDetailActivity.class.getName(), null, false);
        try {
            instrumentation.runOnMainSync(() -> ProductDetailActivity.open(context, item));
            Activity activity = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
            assertNotNull(activity); activities.add(activity); instrumentation.waitForIdleSync(); return activity;
        } finally { instrumentation.removeMonitor(monitor); }
    }
    private void openActivity(Class<? extends Activity> type) {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(type.getName(), null, false);
        try {
            instrumentation.runOnMainSync(() -> context.startActivity(new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            Activity activity = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
            assertNotNull(activity); activities.add(activity); instrumentation.waitForIdleSync();
        } finally { instrumentation.removeMonitor(monitor); }
    }
    private static ViewAction secondSpecification() {
        return new ViewAction() {
            public Matcher<View> getConstraints() { return isAssignableFrom(android.widget.LinearLayout.class); }
            public String getDescription() { return "select the second SKU specification"; }
            public void perform(UiController controller, View view) {
                ((android.widget.LinearLayout) view).getChildAt(1).performClick(); controller.loopMainThreadUntilIdle();
            }
        };
    }
    private static ViewAction rowAction(String title, int childId, ViewAction action) {
        return new ViewAction() {
            public Matcher<View> getConstraints() { return isAssignableFrom(RecyclerView.class); }
            public String getDescription() { return "operate cart row " + title; }
            public void perform(UiController controller, View view) {
                RecyclerView list = (RecyclerView) view;
                for (int i = 0; i < list.getChildCount(); i++) {
                    View row = list.getChildAt(i); TextView label = row.findViewById(R.id.tv_title);
                    if (label != null && title.contentEquals(label.getText())) {
                        View target = childId == 0 ? row : row.findViewById(childId);
                        if (childId != 0) target.performClick();
                        else action.perform(controller, target);
                        controller.loopMainThreadUntilIdle(); return;
                    }
                }
                throw new AssertionError("Cart row not visible: " + title);
            }
        };
    }
    private void screenshot(String name) throws Exception {
        instrumentation.waitForIdleSync();
        Bitmap image = instrumentation.getUiAutomation().takeScreenshot(); assertNotNull(image);
        File directory = new File(context.getExternalFilesDir(null), "lab-evidence");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream stream = new FileOutputStream(new File(directory, name))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally { image.recycle(); }
    }
    @SuppressWarnings("unchecked") private static void restore(SharedPreferences preferences, Map<String, ?> values) {
        SharedPreferences.Editor edit = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey(); Object value = entry.getValue();
            if (value instanceof String) edit.putString(key, (String) value);
            else if (value instanceof Integer) edit.putInt(key, (Integer) value);
            else if (value instanceof Long) edit.putLong(key, (Long) value);
            else if (value instanceof Float) edit.putFloat(key, (Float) value);
            else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof Set) edit.putStringSet(key, (Set<String>) value);
        }
        assertTrue(edit.commit());
    }
}
