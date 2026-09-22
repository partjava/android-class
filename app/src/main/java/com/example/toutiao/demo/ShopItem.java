package com.example.toutiao.demo;

/**
 * 商城商品数据模型。
 * 图片仍然用本地 drawable 资源，和全项目"不联网、无后台"的原则保持一致。
 */
public class ShopItem {
    private final String title;    // 商品标题
    private final double price;    // 价格（元）
    private final int sales;       // 已售件数
    private final int imgRes;      // 商品图资源 id
    private final String category; // 分类：数码/家居/服饰/食品，商城频道标签按它过滤

    public ShopItem(String title, double price, int sales, int imgRes, String category) {
        this.title = title;
        this.price = price;
        this.sales = sales;
        this.imgRes = imgRes;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public double getPrice() {
        return price;
    }

    public int getSales() {
        return sales;
    }

    public int getImgRes() {
        return imgRes;
    }

    public String getCategory() {
        return category;
    }
}
