package com.example.toutiao.demo;

/** 兼容旧入口，所有主页面都由统一 Fragment 容器承载。 */
public class ShopActivity extends MainActivity {
    @Override protected int initialPage() { return R.id.nav_shop; }
}
