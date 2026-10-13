package com.example.toutiao.demo;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.Locale;

/** Displays upstream fields without substituting invented shop promises or reviews. */
final class ProductDetailsRenderer {
    private static void text(Activity activity, int id, String value) {
        ((TextView)activity.findViewById(id)).setText(value);
    }
    private static String value(JSONObject data, String key) {
        return display(data.opt(key));
    }
    private static String display(Object raw) {
        if (raw == null || raw == JSONObject.NULL) return "未提供";
        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray)raw;
            java.util.List<String> values = new java.util.ArrayList<>();
            for (int i = 0; i < array.length(); i++) values.add(display(array.opt(i)));
            return values.isEmpty() ? "未提供" : android.text.TextUtils.join("、", values);
        }
        if (raw instanceof JSONObject) {
            JSONObject object = (JSONObject)raw;
            java.util.List<String> values = new java.util.ArrayList<>();
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) { String key = keys.next(); values.add(label(key) + "：" + display(object.opt(key))); }
            return values.isEmpty() ? "未提供" : android.text.TextUtils.join("\n", values);
        }
        return raw.toString().isEmpty() ? "未提供" : raw.toString();
    }
    static void render(Activity activity, ShopItem item) {
        JSONObject data = item.getDetails();
        String rating = data.has("rating") ? String.format(Locale.CHINA, "评分 %.2f / 5", data.optDouble("rating")) : "评分未提供";
        text(activity, R.id.tv_price_main, String.format(Locale.CHINA, "¥%.2f", item.getPrice()));
        text(activity, R.id.tv_price_origin, data.has("price") ? "原币种价格 USD " + value(data, "price") : "原币种价格未提供");
        text(activity, R.id.tv_stock, "库存 " + value(data, "stock") + " · " + value(data, "availabilityStatus"));
        text(activity, R.id.tv_price_sales, rating + " · 展示数据");
        text(activity, R.id.tv_detail_title, item.getTitle());
        text(activity, R.id.tv_tag_1, value(data, "brand"));
        text(activity, R.id.tv_tag_2, item.getCategory());
        text(activity, R.id.tv_tag_3, data.has("discountPercentage") ? "折扣 " + value(data, "discountPercentage") + "%" : "折扣未提供");
        text(activity, R.id.tv_service_ship, "保修：" + value(data, "warrantyInformation") + "\n退货：" + value(data, "returnPolicy"));
        text(activity, R.id.tv_service_delivery, "运输：" + value(data, "shippingInformation"));
        text(activity, R.id.tv_service_spec, "SKU：" + value(data, "sku") + " · 最小订购量：" + value(data, "minimumOrderQuantity"));

        JSONArray reviews = data.optJSONArray("reviews");
        int reviewCount = reviews == null ? 0 : reviews.length();
        text(activity, R.id.tv_review_header, "商品评价 (" + reviewCount + ")");
        text(activity, R.id.tv_review_rate, rating);
        renderReviewBatch(activity, reviews, 0);

        String brand = value(data, "brand");
        text(activity, R.id.tv_store_avatar, brand.substring(0, 1));
        text(activity, R.id.tv_store_name, "品牌：" + brand);
        text(activity, R.id.tv_store_score, rating + " · 来源 DummyJSON");
        activity.findViewById(R.id.btn_enter_store).setVisibility(View.GONE);
        text(activity, R.id.tv_desc_body1, "商品介绍\n" + value(data, "description"));
        text(activity, R.id.tv_desc_body2, "商品标签\n" + value(data, "tags"));
        text(activity, R.id.tv_desc_body3, "DummyJSON 在线展示数据。人民币价格按 1 USD = ¥7.2 换算，仅用于展示。库存、评价及服务说明来自测试接口。");
        ImageView photo = activity.findViewById(R.id.iv_desc_photo);
        if (!item.getImageUrls().isEmpty()) RemoteImage.load(photo, item.getImageUrls().get(0));
        else { photo.setTag(null); photo.setImageResource(item.getImgRes()); }

        LinearLayout params = activity.findViewById(R.id.ll_desc_params);
        params.removeAllViews();
        addParameter(activity, params, "商品编号", item.getProductId());
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (java.util.Arrays.asList("id", "title", "price", "description", "thumbnail", "images", "reviews").contains(key)) continue;
            addParameter(activity, params, label(key), value(data, key));
        }
    }
    private static String label(String key) {
        switch (key) {
            case "brand": return "品牌"; case "sku": return "SKU"; case "category": return "原始分类";
            case "rating": return "评分"; case "stock": return "库存"; case "discountPercentage": return "折扣百分比";
            case "weight": return "重量（源数据值）"; case "dimensions": return "尺寸（源数据值）";
            case "tags": return "标签"; case "warrantyInformation": return "保修说明";
            case "shippingInformation": return "运输说明"; case "returnPolicy": return "退货规则";
            case "availabilityStatus": return "库存状态"; case "minimumOrderQuantity": return "最小订购量";
            case "width": return "宽"; case "height": return "高"; case "depth": return "深";
            case "createdAt": return "创建时间"; case "updatedAt": return "更新时间";
            case "barcode": return "条形码"; case "qrCode": return "二维码地址";
            case "reviewerEmail": return "评价者邮箱";
            case "meta": return "商品元数据"; default: return key;
        }
    }
    private static void addParameter(Activity activity, LinearLayout parent, String label, String value) {
        TextView row = new TextView(activity);
        row.setText(label + "：" + value);
        row.setTextSize(13);
        int padding = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        row.setPadding(0, padding, 0, padding);
        row.setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.text_secondary));
        parent.addView(row);
    }

    static void renderReviewBatch(Activity activity, JSONArray reviews, int offset) {
        LinearLayout rows = activity.findViewById(R.id.ll_review_rows);
        View btnRefresh = activity.findViewById(R.id.btn_review_refresh);
        rows.removeAllViews();
        if (reviews == null || reviews.length() == 0) {
            TextView empty = new TextView(activity);
            empty.setText("暂无评价");
            rows.addView(empty);
            if (btnRefresh != null) {
                btnRefresh.setVisibility(View.GONE);
            }
            return;
        }

        if (btnRefresh != null) {
            btnRefresh.setVisibility(View.VISIBLE);
            btnRefresh.setTag(offset);
            btnRefresh.setOnClickListener(v -> {
                int nextOffset = (offset + 5) % reviews.length();
                renderReviewBatch(activity, reviews, nextOffset);
            });
        }

        int count = Math.min(5, reviews.length());
        for (int i = 0; i < count; i++) {
            int idx = (offset + i) % reviews.length();
            JSONObject review = reviews.optJSONObject(idx);
            if (review == null) continue;
            View row = LayoutInflater.from(activity).inflate(R.layout.item_detail_review, rows, false);
            String name = value(review, "reviewerName");
            ((TextView)row.findViewById(R.id.tv_review_avatar)).setText(name.isEmpty() ? "匿" : name.substring(0, 1));
            ((TextView)row.findViewById(R.id.tv_review_name)).setText(name);
            StringBuilder reviewText = new StringBuilder("评分 " + value(review, "rating") + " / 5\n" + value(review, "comment"));
            Iterator<String> reviewKeys = review.keys();
            while (reviewKeys.hasNext()) {
                String key = reviewKeys.next();
                if (!java.util.Arrays.asList("reviewerName", "rating", "comment", "date").contains(key)) {
                    reviewText.append("\n").append(label(key)).append("：").append(value(review, key));
                }
            }
            ((TextView)row.findViewById(R.id.tv_review_text)).setText(reviewText);
            String date = value(review, "date");
            ((TextView)row.findViewById(R.id.tv_review_date)).setText(date.substring(0, Math.min(10, date.length())));
            rows.addView(row);
        }
    }
}
