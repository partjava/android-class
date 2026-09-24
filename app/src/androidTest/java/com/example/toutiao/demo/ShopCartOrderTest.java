package com.example.toutiao.demo;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ShopCartOrderTest {

    private Context context;
    private ShopStore store;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        store = new ShopStore(context);
        store.clearCart();
        for (ShopStore.OrderItem oi : store.getOrders()) {
            store.deleteOrder(oi.orderId);
        }
    }

    @Test
    public void cartAddUpdateAndCalculate() {
        assertEquals(0, store.getCartCount());

        // 添加商品1
        store.addToCart("无线蓝牙耳机", 12900, R.drawable.shop_1, "数码", 1);
        assertEquals(1, store.getCartCount());

        // 再次添加商品1（数量应累加）
        store.addToCart("无线蓝牙耳机", 12900, R.drawable.shop_1, "数码", 2);
        List<ShopStore.CartItem> items = store.getCartItems();
        assertEquals(1, items.size());
        assertEquals(3, items.get(0).quantity);
        assertEquals(387.00, items.get(0).getSubtotal(), 0.01);

        // 添加商品2
        store.addToCart("不锈钢保温杯", 5990, R.drawable.shop_2, "家居", 1);
        assertEquals(2, store.getCartItems().size());
        assertEquals(4, store.getCartCount());

        // 更新数量
        store.updateQuantity("不锈钢保温杯", 2);
        items = store.getCartItems();
        for (ShopStore.CartItem ci : items) {
            if ("不锈钢保温杯".equals(ci.title)) {
                assertEquals(2, ci.quantity);
            }
        }

        // 移除单件
        store.removeFromCart("不锈钢保温杯");
        assertEquals(1, store.getCartItems().size());
        assertEquals("无线蓝牙耳机", store.getCartItems().get(0).title);
    }

    @Test
    public void orderLifecycleAndPersistence() {
        // 加入购物车
        store.addToCart("智能运动手表", 29900, R.drawable.shop_6, "数码", 1);
        store.addToCart("香脆经典原味薯片", 1990, R.drawable.shop_7, "食品", 2);

        List<ShopStore.CartItem> buyItems = store.getCartItems();
        assertEquals(2, buyItems.size());

        // 创建订单
        ShopStore.OrderItem order = store.createOrder(
                buyItems,
                "张三",
                "13812345678",
                "北京市海淀区中关村南大街1号",
                1500 // 优惠 15 元
        );

        assertNotNull(order);
        assertNotNull(order.orderId);
        assertEquals("待发货", order.status);
        assertEquals("张三", order.receiverName);
        assertEquals(2, order.items.size());
        assertEquals(338.80, order.getTotal(), 0.01);
        assertEquals(15.00, order.getDiscount(), 0.01);
        assertEquals(323.80, order.getActual(), 0.01);

        // 购物车已结算商品已被清空
        assertEquals(0, store.getCartItems().size());

        // 状态流转: 待发货 -> 待收货 -> 已完成
        store.updateOrderStatus(order.orderId, "待收货");
        ShopStore freshStore = new ShopStore(context);
        ShopStore.OrderItem reloaded = freshStore.getOrder(order.orderId);
        assertNotNull(reloaded);
        assertEquals("待收货", reloaded.status);

        freshStore.updateOrderStatus(order.orderId, "已完成");
        assertEquals("已完成", freshStore.getOrder(order.orderId).status);

        // 模拟退款
        freshStore.updateOrderStatus(order.orderId, "已退款");
        assertEquals("已退款", freshStore.getOrder(order.orderId).status);

        // 删除订单
        freshStore.deleteOrder(order.orderId);
        assertNull(freshStore.getOrder(order.orderId));
    }
}
