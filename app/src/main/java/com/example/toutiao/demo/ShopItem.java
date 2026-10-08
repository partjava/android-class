package com.example.toutiao.demo;

import java.io.Serializable;

/**
 * 商城商品数据模型。
 * 图片仍然用本地 drawable 资源，和全项目"不联网、无后台"的原则保持一致。
 *
 * 实现 Serializable 是为了能整个通过 Intent extra 传给商品详情页
 * （ProductDetailActivity）：5 个字段全是 String/基本类型，序列化没有风险，
 * 也不必像购物车那样拆成 4 个散装 extra 再拼回来。
 */
public class ShopItem implements Serializable {
    private final String productId;
    private final String title;    // 商品标题
    private final double price;    // 价格（元）
    private final int sales;       // 已售件数
    private final int imgRes;      // 商品图资源 id
    private final String category; // 分类：数码/家居/服饰/食品，商城频道标签按它过滤

    public ShopItem(String title, double price, int sales, int imgRes, String category) {
        this(legacyProductId(imgRes, title, category), title, price, sales, imgRes, category);
    }

    public ShopItem(String productId, String title, double price, int sales, int imgRes, String category) {
        if (productId == null || productId.isEmpty()) throw new IllegalArgumentException("商品ID不能为空");
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.sales = sales;
        this.imgRes = imgRes;
        this.category = category;
    }

    public String getProductId() { return productId; }

    public static String legacyProductId(int image, String title, String category) {
        // Fixed historical labels recover legacy IDs even when drawable integers change between APKs.
        String[] titles = {"无线蓝牙耳机 半入耳式 超长续航 通话降噪","316不锈钢保温杯 大容量便携车载水杯","静音机械键盘 87键 背光游戏电竞机械键盘","天然植物精油香薰蜡烛 舒缓安睡玻璃杯香氛","纯棉舒适四件套 亲肤透气全棉双人床上用品","智能运动手表 心率睡眠健康监测 多功能手环","香脆经典原味薯片 休闲膨化零食小吃大礼包","复古护眼台灯 墨绿复古银行灯 桌面床头阅读灯","日式精致便当盒 营养健康便当分格餐盒","轻奢商务双肩包 男女同款 大容量通勤电脑背包","便携手持小风扇 USB充电静音桌面风扇","趣味变色马克杯 创意陶瓷咖啡杯 情侣水杯","10000mAh双向快充移动电源 便携超薄充电宝","人体工学无线鼠标 静音办公笔记本台式鼠标","铝合金桌面手机支架 升降折叠便携平板底座","家用静音空气加湿器 卧室大雾量香薰氛围机","日式复古陶瓷餐具碗碟套组 家用耐热饭碗","超轻全自动晴雨两用伞 防晒防紫外线太阳伞","慢回弹记忆棉U型枕 差旅午睡透气便携颈枕","植萃滋养修护护手霜 补水保湿清爽不油腻","深层水润保湿滋养面霜 清爽修护肌底保湿霜","每日混合坚果大礼包 孕妇健康休闲零食干果","意式烘焙新鲜纯黑咖啡豆 浓郁醇香手冲咖啡","高钙纯牛奶 营养早餐全脂鲜奶 250ml整箱装","220g重磅纯棉纯白短袖T恤 男女百搭打底衫","超轻透气减震运动跑步鞋 防滑耐磨软底休闲鞋","户外防晒遮阳渔夫帽 抽绳可折叠大檐太阳帽","美式复古连帽卫衣 男女同款 加绒保暖宽松外套","Type-C八合一多功能扩展坞 4K高清高速读卡器","户外便携折叠露营椅 超轻钓鱼写生野营靠背椅"};
        String[] categories = {"数码","家居","数码","家居","家居","数码","食品","家居","食品","服饰","数码","家居","数码","数码","数码","家居","家居","服饰","家居","美妆","美妆","食品","食品","食品","服饰","服饰","服饰","服饰","数码","家居"};
        for (int i = 0; i < titles.length; i++) {
            if (titles[i].equals(title) || (title != null && title.length() >= 4 && titles[i].startsWith(title)
                    && categories[i].equals(category))) return "catalog-" + (i + 1);
        }
        int[] images = {R.drawable.shop_1,R.drawable.shop_2,R.drawable.shop_3,R.drawable.shop_4,R.drawable.shop_5,R.drawable.shop_6,R.drawable.shop_7,R.drawable.shop_8,R.drawable.shop_9,R.drawable.shop_10,R.drawable.shop_11,R.drawable.shop_12,R.drawable.shop_13,R.drawable.shop_14,R.drawable.shop_15,R.drawable.shop_16,R.drawable.shop_17,R.drawable.shop_18,R.drawable.shop_19,R.drawable.shop_20,R.drawable.shop_21,R.drawable.shop_22,R.drawable.shop_23,R.drawable.shop_24,R.drawable.shop_25,R.drawable.shop_26,R.drawable.shop_27,R.drawable.shop_28,R.drawable.shop_29,R.drawable.shop_30};
        for (int i = 0; i < images.length; i++) if (images[i] == image) return "catalog-" + (i + 1);
        return "legacy-" + java.util.UUID.nameUUIDFromBytes((title + "|" + category).getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
