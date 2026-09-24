package com.example.toutiao.demo;
/** 兼容旧入口。 */
public class MineActivity extends MainActivity {
    @Override protected int initialPage() { return R.id.nav_mine; }
}
