package com.example.toutiao.demo;

import android.content.Context;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager2.widget.ViewPager2;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProductDetailsDisplayTest {
    @Test public void yanxuanUsesCnyAndCorrectSourceWithoutInventedDetails() {
        Context context = ApplicationProvider.getApplicationContext();
        ShopItem item = new ShopItem("yanxuan-10086", "官网商品", 179, 0, R.drawable.ic_shop, "数码", "",
                "{\"source\":\"网易严选\",\"currency\":\"CNY\",\"price\":179,\"description\":\"官网描述\"}");
        try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, item))) {
            scenario.onActivity(activity -> {
                String price = ((TextView)activity.findViewById(R.id.tv_price_origin)).getText().toString();
                assertFalse(price.contains("USD"));
                assertTrue(((TextView)activity.findViewById(R.id.tv_store_score)).getText().toString().contains("网易严选"));
                assertFalse(((TextView)activity.findViewById(R.id.tv_desc_body3)).getText().toString().contains("DummyJSON"));
                assertTrue(((TextView)activity.findViewById(R.id.tv_desc_body1)).getText().toString().contains("官网描述"));
                assertEquals("评分未提供", ((TextView)activity.findViewById(R.id.tv_review_rate)).getText().toString());
                assertEquals("商品评价 (0)", ((TextView)activity.findViewById(R.id.tv_review_header)).getText().toString());
            });
        }
    }
    @Test public void topControlsStayVisibleAfterScrollingAndBackWorks() {
        Context context = ApplicationProvider.getApplicationContext();
        ShopItem item = new ShopItem("dummy-nav", "固定导航商品", 1, 0, R.drawable.ic_shop, "其他", "", "{}");
        try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, item))) {
            scenario.onActivity(activity -> {
                android.view.View back = activity.findViewById(R.id.btn_gallery_back);
                android.view.View tabs = activity.findViewById(R.id.ll_detail_tabs);
                for (android.view.View control : new android.view.View[]{back, tabs}) {
                    android.view.ViewParent parent = control.getParent();
                    while (parent != null) {
                        assertFalse("Top controls must be outside scrollable content", parent instanceof androidx.core.widget.NestedScrollView);
                        parent = parent.getParent();
                    }
                }
                int[] before = new int[2], after = new int[2];
                back.getLocationOnScreen(before);
                androidx.core.widget.NestedScrollView scroll = activity.findViewById(R.id.nsv_detail);
                scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
                back.getLocationOnScreen(after);
                assertArrayEquals(before, after);
                assertTrue(back.getGlobalVisibleRect(new android.graphics.Rect()));
                assertTrue(tabs.getGlobalVisibleRect(new android.graphics.Rect()));
                back.performClick();
                assertTrue(activity.isFinishing());
            });
        }
    }
    @Test public void fullDetailsSurviveIntentAndRenderAllSourceReviews() throws Exception {
        JSONObject details = new JSONObject("{\"title\":\"接口商品\",\"description\":\"来自接口的详细介绍\",\"brand\":\"接口品牌\",\"rating\":4.2,\"stock\":23,\"price\":10,\"sku\":\"SKU-42\",\"dimensions\":{\"width\":12},\"images\":[\"https://example.com/a.png\",\"https://example.com/b.png\"],\"reviews\":[{\"reviewerName\":\"读者甲\",\"rating\":5,\"comment\":\"接口评价甲\",\"date\":\"2026-10-13\"},{\"reviewerName\":\"读者乙\",\"rating\":3,\"comment\":\"接口评价乙\"}],\"shippingInformation\":\"Ships in 1 week\",\"returnPolicy\":\"30 days return policy\"}");
        ShopItem item = new ShopItem("dummy-42", "接口商品", 72, 0, R.drawable.ic_shop, "家居", "", details.toString());
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        new java.io.ObjectOutputStream(bytes).writeObject(item);
        ShopItem copy = (ShopItem)new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())).readObject();
        assertEquals(2, copy.getImageUrls().size());
        Context context = ApplicationProvider.getApplicationContext();
        try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, copy))) {
            scenario.onActivity(activity -> {
                assertEquals(2, ((ViewPager2)activity.findViewById(R.id.vp_gallery)).getAdapter().getItemCount());
                assertTrue(((TextView)activity.findViewById(R.id.tv_desc_body1)).getText().toString().contains("来自接口的详细介绍"));
                assertEquals("品牌：接口品牌", ((TextView)activity.findViewById(R.id.tv_store_name)).getText().toString());
                assertTrue(((TextView)activity.findViewById(R.id.tv_stock)).getText().toString().contains("23"));
                LinearLayout rows = activity.findViewById(R.id.ll_review_rows);
                assertEquals(2, rows.getChildCount());
                assertTrue(((TextView)rows.getChildAt(1).findViewById(R.id.tv_review_text)).getText().toString().contains("接口评价乙"));
                assertTrue(((TextView)activity.findViewById(R.id.tv_service_spec)).getText().toString().contains("SKU-42"));
            });
        }
    }
    @Test public void missingDetailsNeverInventInventoryOrPositiveReviews() {
        Context context = ApplicationProvider.getApplicationContext();
        ShopItem item = new ShopItem("dummy-missing", "缺省商品", 1, 0, R.drawable.ic_shop, "其他", "", "{}");
        try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, item))) {
            scenario.onActivity(activity -> {
                assertTrue(((TextView)activity.findViewById(R.id.tv_stock)).getText().toString().contains("未提供"));
                assertEquals("商品评价 (0)", ((TextView)activity.findViewById(R.id.tv_review_header)).getText().toString());
                assertEquals(1, ((ViewPager2)activity.findViewById(R.id.vp_gallery)).getAdapter().getItemCount());
                assertEquals("评分未提供", ((TextView)activity.findViewById(R.id.tv_review_rate)).getText().toString());
            });
        }
    }
    @Test public void reviewsRefreshBatchCyclesFiveAtATime() throws Exception {
        org.json.JSONArray reviews = new org.json.JSONArray();
        for (int i = 0; i < 7; i++) {
            reviews.put(new JSONObject().put("reviewerName", "用户" + i).put("rating", 5).put("comment", "评语" + i));
        }
        JSONObject details = new JSONObject().put("title", "测试多评论").put("reviews", reviews);
        ShopItem item = new ShopItem("dummy-reviews", "多评论商品", 99, 0, R.drawable.ic_shop, "图书", "", details.toString());
        Context context = ApplicationProvider.getApplicationContext();
        try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, item))) {
            scenario.onActivity(activity -> {
                LinearLayout rows = activity.findViewById(R.id.ll_review_rows);
                TextView btnRefresh = activity.findViewById(R.id.btn_review_refresh);
                assertEquals(5, rows.getChildCount());
                assertTrue(((TextView)rows.getChildAt(0).findViewById(R.id.tv_review_name)).getText().toString().contains("用户0"));
                assertEquals(android.view.View.VISIBLE, btnRefresh.getVisibility());

                btnRefresh.performClick();
                assertEquals(5, rows.getChildCount());
                assertTrue(((TextView)rows.getChildAt(0).findViewById(R.id.tv_review_name)).getText().toString().contains("用户5"));
                assertTrue(((TextView)rows.getChildAt(1).findViewById(R.id.tv_review_name)).getText().toString().contains("用户6"));
            });
        }
    }
}
