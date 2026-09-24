package com.example.toutiao.demo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 商城购物车与订单本地持久化存储
 */
public final class ShopStore {
    private static final String PREF_NAME = "shop_store_v2";
    private static final String KEY_CART = "cart_items";
    private static final String KEY_ORDERS = "order_items";

    public static class CartItem {
        public String title;
        public long priceCents;
        public int quantity;
        public int imgRes;
        public String category;
        public boolean selected;

        public CartItem(String title, long priceCents, int quantity, int imgRes, String category, boolean selected) {
            this.title = title;
            this.priceCents = priceCents;
            this.quantity = Math.max(1, quantity);
            this.imgRes = imgRes;
            this.category = category;
            this.selected = selected;
        }

        public double getPrice() {
            return priceCents / 100.0;
        }

        public double getSubtotal() {
            return (priceCents * quantity) / 100.0;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("title", title);
                obj.put("priceCents", priceCents);
                obj.put("quantity", quantity);
                obj.put("imgRes", imgRes);
                obj.put("category", category);
                obj.put("selected", selected);
            } catch (Exception ignored) {
            }
            return obj;
        }

        public static CartItem fromJson(JSONObject obj) {
            if (obj == null) return null;
            return new CartItem(
                    obj.optString("title", ""),
                    obj.optLong("priceCents", 0),
                    obj.optInt("quantity", 1),
                    obj.optInt("imgRes", 0),
                    obj.optString("category", ""),
                    obj.optBoolean("selected", true)
            );
        }
    }

    public static class OrderItem {
        public String orderId;
        public String createTime;
        public String status; // 待发货, 待收货, 已完成, 已退款
        public long totalCents;
        public long discountCents;
        public long actualCents;
        public String receiverName;
        public String receiverPhone;
        public String receiverAddress;
        public String expressCompany;
        public String expressNumber;
        public List<CartItem> items;

        public OrderItem() {
            items = new ArrayList<>();
        }

        public double getTotal() {
            return totalCents / 100.0;
        }

        public double getDiscount() {
            return discountCents / 100.0;
        }

        public double getActual() {
            return actualCents / 100.0;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("orderId", orderId);
                obj.put("createTime", createTime);
                obj.put("status", status);
                obj.put("totalCents", totalCents);
                obj.put("discountCents", discountCents);
                obj.put("actualCents", actualCents);
                obj.put("receiverName", receiverName);
                obj.put("receiverPhone", receiverPhone);
                obj.put("receiverAddress", receiverAddress);
                obj.put("expressCompany", expressCompany);
                obj.put("expressNumber", expressNumber);

                JSONArray arr = new JSONArray();
                for (CartItem it : items) {
                    arr.put(it.toJson());
                }
                obj.put("items", arr);
            } catch (Exception ignored) {
            }
            return obj;
        }

        public static OrderItem fromJson(JSONObject obj) {
            if (obj == null) return null;
            OrderItem item = new OrderItem();
            item.orderId = obj.optString("orderId", "");
            item.createTime = obj.optString("createTime", "");
            item.status = obj.optString("status", "待发货");
            item.totalCents = obj.optLong("totalCents", 0);
            item.discountCents = obj.optLong("discountCents", 0);
            item.actualCents = obj.optLong("actualCents", 0);
            item.receiverName = obj.optString("receiverName", "张同学");
            item.receiverPhone = obj.optString("receiverPhone", "138****0001");
            item.receiverAddress = obj.optString("receiverAddress", "北京市海淀区中关村南大街1号");
            item.expressCompany = obj.optString("expressCompany", "顺丰速运");
            item.expressNumber = obj.optString("expressNumber", "SF" + System.currentTimeMillis() % 1000000000L);

            JSONArray arr = obj.optJSONArray("items");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    CartItem ci = CartItem.fromJson(arr.optJSONObject(i));
                    if (ci != null) item.items.add(ci);
                }
            }
            return item;
        }
    }

    private final SharedPreferences prefs;

    public ShopStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        migrateLegacyData(context);
    }

    private void migrateLegacyData(Context context) {
        // 迁移旧版 ContentStore 中的购物车与订单数据
        if (prefs.getBoolean("migrated_legacy", false)) return;
        try {
            ContentStore legacy = new ContentStore(context);
            JSONArray legacyCart = legacy.list("cart");
            if (legacyCart.length() > 0 && !prefs.contains(KEY_CART)) {
                List<CartItem> list = new ArrayList<>();
                for (int i = 0; i < legacyCart.length(); i++) {
                    JSONObject obj = legacyCart.optJSONObject(i);
                    if (obj != null) {
                        list.add(new CartItem(
                                obj.optString("title"),
                                obj.optLong("cents", 0),
                                obj.optInt("quantity", 1),
                                obj.optInt("img", 0),
                                "推荐",
                                true
                        ));
                    }
                }
                saveCart(list);
            }
        } catch (Exception ignored) {
        }
        prefs.edit().putBoolean("migrated_legacy", true).apply();
    }

    // ====== 购物车操作 ======

    public synchronized List<CartItem> getCartItems() {
        List<CartItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_CART, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                CartItem ci = CartItem.fromJson(arr.optJSONObject(i));
                if (ci != null) list.add(ci);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public synchronized void saveCart(List<CartItem> list) {
        JSONArray arr = new JSONArray();
        for (CartItem it : list) {
            arr.put(it.toJson());
        }
        prefs.edit().putString(KEY_CART, arr.toString()).apply();
    }

    public synchronized void addToCart(String title, long priceCents, int imgRes, String category, int addQty) {
        List<CartItem> list = getCartItems();
        boolean found = false;
        for (CartItem it : list) {
            if (it.title.equals(title)) {
                it.quantity += addQty;
                it.selected = true;
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(0, new CartItem(title, priceCents, addQty, imgRes, category, true));
        }
        saveCart(list);
    }

    public synchronized void updateQuantity(String title, int newQty) {
        List<CartItem> list = getCartItems();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).title.equals(title)) {
                if (newQty <= 0) {
                    list.remove(i);
                } else {
                    list.get(i).quantity = newQty;
                }
                break;
            }
        }
        saveCart(list);
    }

    public synchronized void setItemSelected(String title, boolean selected) {
        List<CartItem> list = getCartItems();
        for (CartItem it : list) {
            if (it.title.equals(title)) {
                it.selected = selected;
                break;
            }
        }
        saveCart(list);
    }

    public synchronized void selectAll(boolean select) {
        List<CartItem> list = getCartItems();
        for (CartItem it : list) {
            it.selected = select;
        }
        saveCart(list);
    }

    public synchronized void removeFromCart(String title) {
        List<CartItem> list = getCartItems();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).title.equals(title)) {
                list.remove(i);
                break;
            }
        }
        saveCart(list);
    }

    public synchronized void removeSelectedFromCart() {
        List<CartItem> list = getCartItems();
        List<CartItem> remaining = new ArrayList<>();
        for (CartItem it : list) {
            if (!it.selected) {
                remaining.add(it);
            }
        }
        saveCart(remaining);
    }

    public synchronized void clearCart() {
        prefs.edit().remove(KEY_CART).apply();
    }

    public synchronized int getCartCount() {
        int count = 0;
        for (CartItem it : getCartItems()) {
            count += it.quantity;
        }
        return count;
    }

    // ====== 订单操作 ======

    public synchronized List<OrderItem> getOrders() {
        List<OrderItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_ORDERS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                OrderItem oi = OrderItem.fromJson(arr.optJSONObject(i));
                if (oi != null) list.add(oi);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public synchronized void saveOrders(List<OrderItem> list) {
        JSONArray arr = new JSONArray();
        for (OrderItem it : list) {
            arr.put(it.toJson());
        }
        prefs.edit().putString(KEY_ORDERS, arr.toString()).apply();
    }

    public synchronized OrderItem getOrder(String orderId) {
        for (OrderItem it : getOrders()) {
            if (it.orderId.equals(orderId)) return it;
        }
        return null;
    }

    public synchronized OrderItem createOrder(List<CartItem> buyItems, String receiverName, String phone, String address, long discountCents) {
        OrderItem order = new OrderItem();
        String dateStr = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        order.orderId = "TT" + dateStr + (int) (Math.random() * 900 + 100);
        order.createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
        order.status = "待发货";
        order.receiverName = (receiverName == null || receiverName.trim().isEmpty()) ? "李同学" : receiverName.trim();
        order.receiverPhone = (phone == null || phone.trim().isEmpty()) ? "13800138000" : phone.trim();
        order.receiverAddress = (address == null || address.trim().isEmpty()) ? "湖北省武汉市东湖高新区光谷软件园F座" : address.trim();
        order.expressCompany = "顺丰速运";
        order.expressNumber = "SF" + (System.currentTimeMillis() % 10000000000L);
        order.items.addAll(buyItems);

        long sum = 0;
        for (CartItem it : buyItems) {
            sum += (it.priceCents * it.quantity);
        }
        order.totalCents = sum;
        order.discountCents = Math.min(discountCents, sum);
        order.actualCents = Math.max(0, sum - order.discountCents);

        List<OrderItem> list = getOrders();
        list.add(0, order);
        saveOrders(list);

        // 从购物车移除已购买商品
        List<CartItem> cart = getCartItems();
        for (CartItem bought : buyItems) {
            for (int i = 0; i < cart.size(); i++) {
                if (cart.get(i).title.equals(bought.title)) {
                    cart.remove(i);
                    break;
                }
            }
        }
        saveCart(cart);

        return order;
    }

    public synchronized void updateOrderStatus(String orderId, String newStatus) {
        List<OrderItem> list = getOrders();
        for (OrderItem it : list) {
            if (it.orderId.equals(orderId)) {
                it.status = newStatus;
                break;
            }
        }
        saveOrders(list);
    }

    public synchronized void deleteOrder(String orderId) {
        List<OrderItem> list = getOrders();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).orderId.equals(orderId)) {
                list.remove(i);
                break;
            }
        }
        saveOrders(list);
    }
}
