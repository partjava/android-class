package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * 商城购物弹窗与页面快捷路由
 */
public final class ShoppingDialogs {
    private ShoppingDialogs() { }

    public static void detail(Context c, ShopItem item) {
        View view = LayoutInflater.from(c).inflate(R.layout.item_cart_product, null, false);
        view.findViewById(R.id.cb_select).setVisibility(View.GONE);
        view.findViewById(R.id.btn_minus).setVisibility(View.GONE);
        view.findViewById(R.id.btn_plus).setVisibility(View.GONE);
        view.findViewById(R.id.tv_quantity).setVisibility(View.GONE);

        ImageView image = view.findViewById(R.id.iv_thumb);
        image.setImageResource(item.getImgRes());

        TextView title = view.findViewById(R.id.tv_title);
        title.setText(item.getTitle());

        TextView category = view.findViewById(R.id.tv_category);
        category.setText(item.getCategory() + " · 已售 " + item.getSales());

        TextView price = view.findViewById(R.id.tv_price);
        price.setText("¥" + String.format(Locale.CHINA, "%.2f", item.getPrice()));

        new AlertDialog.Builder(c)
                .setTitle("商品详情")
                .setView(view)
                .setPositiveButton("立即购买", (d, w) -> {
                    Intent intent = new Intent(c, OrderConfirmActivity.class);
                    intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_TITLE, item.getTitle());
                    intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_PRICE, item.getPrice());
                    intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_IMG, item.getImgRes());
                    intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_CAT, item.getCategory());
                    c.startActivity(intent);
                })
                .setNeutralButton("加入购物车", (d, w) -> {
                    ShopStore store = new ShopStore(c);
                    store.addToCart(item.getTitle(), Math.round(item.getPrice() * 100), item.getImgRes(), item.getCategory(), 1);
                    Toast.makeText(c, "已加入购物车", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("查看购物车", (d, w) -> showCart(c))
                .show();
    }

    public static void showCart(Context c) {
        Intent intent = new Intent(c, CartActivity.class);
        c.startActivity(intent);
    }

    public static void showOrders(Context c) {
        Intent intent = new Intent(c, OrderListActivity.class);
        c.startActivity(intent);
    }
}
