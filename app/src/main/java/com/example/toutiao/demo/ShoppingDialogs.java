package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;

/**
 * 商城购物页面快捷路由。
 *
 * 原来这里还有一个 detail() 弹窗（立即购买/加入购物车/查看购物车三个按钮），
 * 是商品卡片唯一的落地页；商品详情页（ProductDetailActivity）上线后它被整体
 * 替代并删除——点击商品卡片现在直接进详情页。这里只保留两个页面直达路由，
 * 我的页九宫格还在用。
 */
public final class ShoppingDialogs {
    private ShoppingDialogs() { }

    public static void showCart(Context c) {
        Intent intent = new Intent(c, CartActivity.class);
        c.startActivity(intent);
    }

    public static void showOrders(Context c) {
        Intent intent = new Intent(c, OrderListActivity.class);
        c.startActivity(intent);
    }
}
