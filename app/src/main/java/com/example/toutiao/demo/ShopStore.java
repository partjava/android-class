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
    private static final String KEY_COUPONS = "user_coupons_v1";
    private static final String KEY_ADDRESSES = "user_addresses_v1";

    public static class AddressItem {
        public String id;
        public String name;
        public String phone;
        public String tag; // "宿舍", "快递点", "教学楼"
        public String fullAddress;
        public boolean isDefault;

        public AddressItem(String id, String name, String phone, String tag, String fullAddress, boolean isDefault) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.tag = tag;
            this.fullAddress = fullAddress;
            this.isDefault = isDefault;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("name", name);
                obj.put("phone", phone);
                obj.put("tag", tag);
                obj.put("fullAddress", fullAddress);
                obj.put("isDefault", isDefault);
            } catch (Exception ignored) {}
            return obj;
        }

        public static AddressItem fromJson(JSONObject obj) {
            if (obj == null) return null;
            return new AddressItem(
                    obj.optString("id", ""),
                    obj.optString("name", ""),
                    obj.optString("phone", ""),
                    obj.optString("tag", "宿舍"),
                    obj.optString("fullAddress", ""),
                    obj.optBoolean("isDefault", false)
            );
        }
    }

    public static class CouponItem {
        public String id;
        public String title;
        public int discountAmount; // 抵扣金额 (元)
        public int minSpend; // 门槛金额 (元)
        public String desc;
        public boolean used;
        public long receiveTime;

        public CouponItem(String id, String title, int discountAmount, int minSpend, String desc, boolean used, long receiveTime) {
            this.id = id;
            this.title = title;
            this.discountAmount = discountAmount;
            this.minSpend = minSpend;
            this.desc = desc;
            this.used = used;
            this.receiveTime = receiveTime;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("title", title);
                obj.put("discountAmount", discountAmount);
                obj.put("minSpend", minSpend);
                obj.put("desc", desc);
                obj.put("used", used);
                obj.put("receiveTime", receiveTime);
            } catch (Exception ignored) {}
            return obj;
        }

        public static CouponItem fromJson(JSONObject obj) {
            if (obj == null) return null;
            return new CouponItem(
                    obj.optString("id", ""),
                    obj.optString("title", ""),
                    obj.optInt("discountAmount", 0),
                    obj.optInt("minSpend", 0),
                    obj.optString("desc", ""),
                    obj.optBoolean("used", false),
                    obj.optLong("receiveTime", System.currentTimeMillis())
            );
        }
    }

    public static class CartItem {
        public String productId;
        public String sku;
        public String title;
        public long priceCents;
        public int quantity;
        public int imgRes;
        public String category;
        public boolean selected;

        public CartItem(String title, long priceCents, int quantity, int imgRes, String category, boolean selected) {
            this(ShopItem.legacyProductId(imgRes, title, category), "默认规格", title, priceCents, quantity, imgRes, category, selected);
        }

        public CartItem(String productId, String sku, String title, long priceCents, int quantity, int imgRes, String category, boolean selected) {
            this.productId = productId;
            this.sku = sku == null || sku.isEmpty() ? "默认规格" : sku;
            this.title = title;
            this.priceCents = Math.min(Long.MAX_VALUE / 99, Math.max(0, priceCents));
            this.quantity = Math.min(99, Math.max(1, quantity));
            this.imgRes = imgRes;
            this.category = category;
            this.selected = selected;
        }

        public String key() { return productId + "|" + sku; }

        public long subtotalCents() {
            if (priceCents < 0 || quantity <= 0 || quantity > 99) throw new IllegalArgumentException("商品金额或数量无效");
            return Math.multiplyExact(priceCents, (long) quantity);
        }

        public double getPrice() {
            return priceCents / 100.0;
        }

        public double getSubtotal() {
            return subtotalCents() / 100.0;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("title", title);
                obj.put("id", key());
                obj.put("productId", productId);
                obj.put("sku", sku);
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
                    obj.optString("productId", ShopItem.legacyProductId(obj.optInt("imgRes", 0), obj.optString("title"), obj.optString("category"))),
                    obj.optString("sku", "默认规格"),
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
        public boolean isEvaluated;
        public int rating; // 1-5星
        public String comment;
        public List<CartItem> items;

        public OrderItem() {
            items = new ArrayList<>();
            isEvaluated = false;
            rating = 5;
            comment = "";
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
                obj.put("id", orderId);
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
                obj.put("isEvaluated", isEvaluated);
                obj.put("rating", rating);
                obj.put("comment", comment);

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
            item.orderId = obj.optString("orderId", obj.optString("id", ""));
            if (item.orderId.isEmpty()) throw new IllegalArgumentException("订单缺少ID");
            item.createTime = obj.optString("createTime", "");
            item.status = obj.optString("status", "待发货");
            item.totalCents = obj.optLong("totalCents", 0);
            item.discountCents = obj.optLong("discountCents", 0);
            item.actualCents = obj.optLong("actualCents", 0);
            item.receiverName = obj.optString("receiverName", "张同学");
            item.receiverPhone = obj.optString("receiverPhone", "138****0001");
            item.receiverAddress = obj.optString("receiverAddress", "湖北省武汉市江夏区 武汉晴川学院 5号宿舍楼402室");
            item.expressCompany = obj.optString("expressCompany", "顺丰速运");
            item.expressNumber = obj.optString("expressNumber", "SF" + System.currentTimeMillis() % 1000000000L);
            item.isEvaluated = obj.optBoolean("isEvaluated", false);
            item.rating = obj.optInt("rating", 5);
            item.comment = obj.optString("comment", "");

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
    private final DemoDatabase database;

    public ShopStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        database = new DemoDatabase(context);
    }

    // ====== 购物车操作 ======

    public List<CartItem> getCartItems() {
        List<CartItem> list = new ArrayList<>();
        {
            JSONArray arr = database.list("cart");
            for (int i = 0; i < arr.length(); i++) {
                CartItem ci = CartItem.fromJson(arr.optJSONObject(i));
                if (ci != null) list.add(ci);
            }
        }
        return list;
    }

    public void saveCart(List<CartItem> list) {
        JSONArray arr = new JSONArray();
        for (CartItem it : list) {
            arr.put(it.toJson());
        }
        database.replace("cart", arr);
    }

    public void addToCart(String title, long priceCents, int imgRes, String category, int addQty) {
        addToCart(ShopItem.legacyProductId(imgRes, title, category), "默认规格", title, priceCents, imgRes, category, addQty);
    }

    public void addToCart(String productId, String sku, String title, long priceCents, int imgRes, String category, int addQty) {
        database.transaction(() -> {
            if (addQty <= 0 || priceCents < 0 || priceCents > Long.MAX_VALUE / 99) throw new IllegalArgumentException("商品金额或数量无效");
            List<CartItem> list = getCartItems();
            String key = new CartItem(productId, sku, title, priceCents, addQty, imgRes, category, true).key();
            boolean found = false;
            for (CartItem it : list) {
                if (it.key().equals(key)) {
                    it.quantity = (int) Math.min(99L, (long) it.quantity + addQty);
                    it.selected = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                list.add(0, new CartItem(productId, sku, title, priceCents, addQty, imgRes, category, true));
            }
            saveCart(list);
            return null;
        });
    }

    public void updateQuantity(String key, int newQty) {
        database.transaction(() -> {
            List<CartItem> list = getCartItems();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).key().equals(key)) {
                    if (newQty <= 0) {
                        list.remove(i);
                    } else {
                        list.get(i).quantity = Math.min(99, newQty);
                    }
                    break;
                }
            }
            saveCart(list);
            return null;
        });
    }

    public void setItemSelected(String key, boolean selected) {
        database.transaction(() -> {
            List<CartItem> list = getCartItems();
            for (CartItem it : list) {
                if (it.key().equals(key)) {
                    it.selected = selected;
                    break;
                }
            }
            saveCart(list);
            return null;
        });
    }

    public void selectAll(boolean select) {
        database.transaction(() -> {
            List<CartItem> list = getCartItems();
            for (CartItem it : list) {
                it.selected = select;
            }
            saveCart(list);
            return null;
        });
    }

    public void removeFromCart(String key) {
        database.transaction(() -> {
            List<CartItem> list = getCartItems();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).key().equals(key)) {
                    list.remove(i);
                    break;
                }
            }
            saveCart(list);
            return null;
        });
    }

    public void removeSelectedFromCart() {
        database.transaction(() -> {
            List<CartItem> list = getCartItems();
            List<CartItem> remaining = new ArrayList<>();
            for (CartItem it : list) {
                if (!it.selected) {
                    remaining.add(it);
                }
            }
            saveCart(remaining);
            return null;
        });
    }

    public void clearCart() {
        database.replace("cart", new JSONArray());
    }

    public int getCartCount() {
        int count = 0;
        for (CartItem it : getCartItems()) {
            count += it.quantity;
        }
        return count;
    }

    // ====== 订单操作 ======

    public List<OrderItem> getOrders() {
        List<OrderItem> list = new ArrayList<>();
        {
            JSONArray arr = database.list("orders");
            for (int i = 0; i < arr.length(); i++) {
                OrderItem oi = OrderItem.fromJson(arr.optJSONObject(i));
                if (oi != null) list.add(oi);
            }
        }
        return list;
    }

    public void saveOrders(List<OrderItem> list) {
        JSONArray arr = new JSONArray();
        for (OrderItem it : list) {
            arr.put(it.toJson());
        }
        database.replace("orders", arr);
    }

    public OrderItem getOrder(String orderId) {
        for (OrderItem it : getOrders()) {
            if (it.orderId.equals(orderId)) return it;
        }
        return null;
    }

    public OrderItem createOrder(List<CartItem> buyItems, String receiverName, String phone, String address, long discountCents) {
        return createOrder(buyItems, receiverName, phone, address, discountCents, true);
    }

    public OrderItem createOrder(List<CartItem> buyItems, String receiverName, String phone, String address, long discountCents, boolean removePurchasedFromCart) {
        return database.transaction(() -> {
            if (buyItems == null || buyItems.isEmpty()) throw new IllegalArgumentException("暂无待结算商品");
            OrderItem order = new OrderItem();
            order.orderId = "TT" + java.util.UUID.randomUUID().toString().replace("-", "");
            order.createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
            order.status = "待发货";
            order.receiverName = (receiverName == null || receiverName.trim().isEmpty()) ? "李同学" : receiverName.trim();
            order.receiverPhone = (phone == null || phone.trim().isEmpty()) ? "13800138000" : phone.trim();
            order.receiverAddress = (address == null || address.trim().isEmpty()) ? "湖北省武汉市东湖高新区光谷软件园F座" : address.trim();
            order.expressCompany = "顺丰速运";
            order.expressNumber = "SF" + (System.currentTimeMillis() % 10000000000L);
            for (CartItem it : buyItems) order.items.add(CartItem.fromJson(it.toJson()));

            long sum = 0;
            for (CartItem it : buyItems) {
                sum = Math.addExact(sum, it.subtotalCents());
            }
            order.totalCents = sum;
            order.discountCents = Math.max(0, Math.min(discountCents, sum));
            order.actualCents = Math.max(0, sum - order.discountCents);

            List<OrderItem> list = getOrders();
            list.add(0, order);
            saveOrders(list);

            // 从购物车移除已购买商品
            List<CartItem> cart = getCartItems();
            if (removePurchasedFromCart) for (CartItem bought : buyItems) {
                for (int i = 0; i < cart.size(); i++) {
                    if (cart.get(i).key().equals(bought.key())) {
                        int remaining = cart.get(i).quantity - bought.quantity;
                        if (remaining <= 0) cart.remove(i); else cart.get(i).quantity = remaining;
                        break;
                    }
                }
            }
            saveCart(cart);

            return order;
        });
    }

    public void updateOrderStatus(String orderId, String newStatus) {
        database.transaction(() -> {
            List<OrderItem> list = getOrders();
            for (OrderItem it : list) {
                if (it.orderId.equals(orderId)) {
                    it.status = newStatus;
                    break;
                }
            }
            saveOrders(list);
            return null;
        });
    }

    public void deleteOrder(String orderId) {
        database.transaction(() -> {
            List<OrderItem> list = getOrders();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).orderId.equals(orderId)) {
                    list.remove(i);
                    break;
                }
            }
            saveOrders(list);
            return null;
        });
    }

    public List<CouponItem> getCoupons() {
        String json = prefs.getString(KEY_COUPONS, null);
        List<CouponItem> list = new ArrayList<>();
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    CouponItem item = CouponItem.fromJson(arr.getJSONObject(i));
                    if (item != null) list.add(item);
                }
            } catch (Exception ignored) {}
        }
        if (list.isEmpty()) {
            CouponItem welcome = new CouponItem("c_welcome", "新人首单全品类立减券", 10, 0, "商城新人专享全场立减", false, System.currentTimeMillis());
            list.add(welcome);
            saveCoupons(list);
        }
        return list;
    }

    public void saveCoupons(List<CouponItem> list) {
        JSONArray arr = new JSONArray();
        for (CouponItem item : list) {
            arr.put(item.toJson());
        }
        prefs.edit().putString(KEY_COUPONS, arr.toString()).apply();
    }

    public boolean addCoupon(int discountAmount, String title, int minSpend, String desc) {
        List<CouponItem> list = getCoupons();
        for (CouponItem it : list) {
            if (it.title.equals(title) && !it.used) {
                return false;
            }
        }
        String id = "c_" + System.currentTimeMillis();
        CouponItem newItem = new CouponItem(id, title, discountAmount, minSpend, desc, false, System.currentTimeMillis());
        list.add(0, newItem);
        saveCoupons(list);
        return true;
    }

    public void markCouponUsed(String couponId) {
        if (couponId == null) return;
        List<CouponItem> list = getCoupons();
        for (CouponItem it : list) {
            if (it.id.equals(couponId)) {
                it.used = true;
                break;
            }
        }
        saveCoupons(list);
    }

    public List<CouponItem> getAvailableCoupons(long orderAmountCents) {
        List<CouponItem> all = getCoupons();
        List<CouponItem> available = new ArrayList<>();
        for (CouponItem c : all) {
            if (!c.used && c.discountAmount > 0 && orderAmountCents >= Math.max(0, c.minSpend) * 100L) {
                available.add(c);
            }
        }
        return available;
    }

    // ====== 收货地址管理 ======

    public List<AddressItem> getAddresses() {
        String json = prefs.getString(KEY_ADDRESSES, null);
        List<AddressItem> list = new ArrayList<>();
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    AddressItem item = AddressItem.fromJson(arr.getJSONObject(i));
                    if (item != null) list.add(item);
                }
            } catch (Exception ignored) {}
        }
        if (list.isEmpty()) {
            list.add(new AddressItem("addr_1", "晴川学子", "13888886666", "宿舍", "武汉市江夏区中华科技产业园 武汉晴川学院 5号宿舍楼402室", true));
            list.add(new AddressItem("addr_2", "李同学", "13912345678", "快递点", "武汉市江夏区 武汉晴川学院 大学生活动中心菜鸟驿站", false));
            list.add(new AddressItem("addr_3", "王同学", "15099887766", "教学楼", "武汉市江夏区 武汉晴川学院 行政教学楼A栋102室", false));
            saveAddresses(list);
        }
        return list;
    }

    public void saveAddresses(List<AddressItem> list) {
        JSONArray arr = new JSONArray();
        for (AddressItem item : list) {
            arr.put(item.toJson());
        }
        prefs.edit().putString(KEY_ADDRESSES, arr.toString()).apply();
    }

    public void addAddress(String name, String phone, String tag, String fullAddress, boolean isDefault) {
        List<AddressItem> list = getAddresses();
        String id = "addr_" + System.currentTimeMillis();
        if (isDefault) {
            for (AddressItem a : list) a.isDefault = false;
        }
        list.add(0, new AddressItem(id, name, phone, tag, fullAddress, isDefault));
        saveAddresses(list);
    }

    public AddressItem getDefaultAddress() {
        List<AddressItem> list = getAddresses();
        for (AddressItem a : list) {
            if (a.isDefault) return a;
        }
        return list.isEmpty() ? null : list.get(0);
    }

    public void setDefaultAddress(String addressId) {
        List<AddressItem> list = getAddresses();
        for (AddressItem a : list) {
            a.isDefault = a.id.equals(addressId);
        }
        saveAddresses(list);
    }

    // ====== 订单全生命周期流转 ======

    public void shipOrder(String orderId) {
        List<OrderItem> list = getOrders();
        for (OrderItem it : list) {
            if (it.orderId.equals(orderId)) {
                it.status = "待收货";
                it.expressCompany = "顺丰速运";
                it.expressNumber = "SF" + (System.currentTimeMillis() % 10000000000L);
                break;
            }
        }
        saveOrders(list);
    }

    public void evaluateOrder(String orderId, int rating, String comment) {
        List<OrderItem> list = getOrders();
        for (OrderItem it : list) {
            if (it.orderId.equals(orderId)) {
                it.isEvaluated = true;
                it.rating = Math.max(1, Math.min(5, rating));
                it.comment = (comment == null ? "" : comment.trim());
                break;
            }
        }
        saveOrders(list);
    }
}
