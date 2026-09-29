package com.example.toutiao.demo;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShopFragment extends PageFragment {
    @Override public View onCreateView(android.view.LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_shop, parent, false);
    }

    private static final String[] TAB_LABELS = {"关注", "推荐", "闪购", "国补", "飞猪", "新风潮", "穿搭"};
    private static final int DEFAULT_TAB = 1;//默认选中"推荐"

    private HorizontalScrollView hsTabs;
    private LinearLayout llTabs, llDots;
    private RecyclerView rvEntries, rvShop;
    private androidx.core.widget.NestedScrollView nsvShop;

    private final List<View> tabViews = new ArrayList<>();
    private int currentTab = DEFAULT_TAB;

    private List<ShopItem> allItems;    //全部商品
    private List<ShopItem> productList; //当前频道的商品，ShopAdapter 持有它的引用（同 HomeActivity 的 newsList）
    private ShopAdapter shopAdapter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindView();
        initShopData();
        setupTabs();
        setupEntryPager();
        bindEvent();
    }

    private void bindView() {
        hsTabs = findViewById(R.id.hs_tabs);
        llTabs = findViewById(R.id.ll_tabs);
        llDots = findViewById(R.id.ll_dots);
        rvEntries = findViewById(R.id.rv_entries);
        rvShop = findViewById(R.id.rv_shop);
        nsvShop = findViewById(R.id.nsv_shop);
    }

    private void initShopData() {
        allItems = buildCatalog();

        productList = new ArrayList<>();
        shopAdapter = new ShopAdapter(productList);

        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvShop.setLayoutManager(layoutManager);
        rvShop.setAdapter(shopAdapter);

        shopAdapter.setOnItemClickListener(item ->
                startActivity(ProductDetailActivity.intent(requireContext(), item)));
    }

    /**
     * 全部商品的目录数据。抽成 static 是因为商品详情页的
     * 「你可能还会喜欢」也要拿同一份目录来挑推荐位——
     * 详情页拿到的是目录里的同一个 ShopItem 实例（走 Intent 序列化
     * 会在另一侧还原出内容相等的对象，引用比较在详情页内不做）。
     */
    static List<ShopItem> buildCatalog() {
        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem("无线蓝牙耳机 半入耳式 超长续航 通话降噪", 129.00, 8632, R.drawable.shop_1, "数码"));
        items.add(new ShopItem("316不锈钢保温杯 大容量便携车载水杯", 59.90, 24310, R.drawable.shop_2, "家居"));
        items.add(new ShopItem("静音机械键盘 87键 背光游戏电竞机械键盘", 219.00, 1531, R.drawable.shop_3, "数码"));
        items.add(new ShopItem("天然植物精油香薰蜡烛 舒缓安睡玻璃杯香氛", 49.90, 2418, R.drawable.shop_4, "家居"));
        items.add(new ShopItem("纯棉舒适四件套 亲肤透气全棉双人床上用品", 189.00, 7612, R.drawable.shop_5, "家居"));
        items.add(new ShopItem("智能运动手表 心率睡眠健康监测 多功能手环", 299.00, 11586, R.drawable.shop_6, "数码"));
        items.add(new ShopItem("香脆经典原味薯片 休闲膨化零食小吃大礼包", 19.90, 15821, R.drawable.shop_7, "食品"));
        items.add(new ShopItem("复古护眼台灯 墨绿复古银行灯 桌面床头阅读灯", 89.00, 954, R.drawable.shop_8, "家居"));
        items.add(new ShopItem("日式精致便当盒 营养健康便当分格餐盒", 29.90, 6534, R.drawable.shop_9, "食品"));
        items.add(new ShopItem("轻奢商务双肩包 男女同款 大容量通勤电脑背包", 159.00, 2078, R.drawable.shop_10, "服饰"));
        items.add(new ShopItem("便携手持小风扇 USB充电静音桌面风扇", 39.90, 8296, R.drawable.shop_11, "数码"));
        items.add(new ShopItem("趣味变色马克杯 创意陶瓷咖啡杯 情侣水杯", 35.00, 1476, R.drawable.shop_12, "家居"));
        items.add(new ShopItem("10000mAh双向快充移动电源 便携超薄充电宝", 79.00, 18450, R.drawable.shop_13, "数码"));
        items.add(new ShopItem("人体工学无线鼠标 静音办公笔记本台式鼠标", 49.00, 12690, R.drawable.shop_14, "数码"));
        items.add(new ShopItem("铝合金桌面手机支架 升降折叠便携平板底座", 29.90, 9380, R.drawable.shop_15, "数码"));
        items.add(new ShopItem("家用静音空气加湿器 卧室大雾量香薰氛围机", 69.00, 5210, R.drawable.shop_16, "家居"));
        items.add(new ShopItem("日式复古陶瓷餐具碗碟套组 家用耐热饭碗", 45.00, 3120, R.drawable.shop_17, "家居"));
        items.add(new ShopItem("超轻全自动晴雨两用伞 防晒防紫外线太阳伞", 39.90, 14500, R.drawable.shop_18, "服饰"));
        items.add(new ShopItem("慢回弹记忆棉U型枕 差旅午睡透气便携颈枕", 38.00, 7860, R.drawable.shop_19, "家居"));
        items.add(new ShopItem("植萃滋养修护护手霜 补水保湿清爽不油腻", 25.00, 21900, R.drawable.shop_20, "美妆"));
        items.add(new ShopItem("深层水润保湿滋养面霜 清爽修护肌底保湿霜", 88.00, 6340, R.drawable.shop_21, "美妆"));
        items.add(new ShopItem("每日混合坚果大礼包 孕妇健康休闲零食干果", 69.90, 26800, R.drawable.shop_22, "食品"));
        items.add(new ShopItem("意式烘焙新鲜纯黑咖啡豆 浓郁醇香手冲咖啡", 58.00, 4890, R.drawable.shop_23, "食品"));
        items.add(new ShopItem("高钙纯牛奶 营养早餐全脂鲜奶 250ml整箱装", 49.90, 38200, R.drawable.shop_24, "食品"));
        items.add(new ShopItem("220g重磅纯棉纯白短袖T恤 男女百搭打底衫", 49.00, 16700, R.drawable.shop_25, "服饰"));
        items.add(new ShopItem("超轻透气减震运动跑步鞋 防滑耐磨软底休闲鞋", 199.00, 5840, R.drawable.shop_26, "服饰"));
        items.add(new ShopItem("户外防晒遮阳渔夫帽 抽绳可折叠大檐太阳帽", 32.00, 8930, R.drawable.shop_27, "服饰"));
        items.add(new ShopItem("美式复古连帽卫衣 男女同款 加绒保暖宽松外套", 139.00, 4710, R.drawable.shop_28, "服饰"));
        items.add(new ShopItem("Type-C八合一多功能扩展坞 4K高清高速读卡器", 109.00, 3420, R.drawable.shop_29, "数码"));
        items.add(new ShopItem("户外便携折叠露营椅 超轻钓鱼写生野营靠背椅", 79.00, 5130, R.drawable.shop_30, "家居"));
        return items;
    }

    private void setupTabs() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < TAB_LABELS.length; i++) {
            View tab = inflater.inflate(R.layout.item_channel_tab, llTabs, false);
            ((TextView) tab.findViewById(R.id.tv_tab_label)).setText(TAB_LABELS[i]);
            final int index = i;
            tab.setOnClickListener(v -> selectTab(index));
            llTabs.addView(tab);
            tabViews.add(tab);
        }
        selectTab(DEFAULT_TAB);
    }

    private void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < tabViews.size(); i++) {
            boolean selected = i == index;
            TextView tv = tabViews.get(i).findViewById(R.id.tv_tab_label);
            View line = tabViews.get(i).findViewById(R.id.v_tab_line);
            tv.setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? R.color.brand_red : R.color.text_primary));
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            line.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }

        View tab = tabViews.get(index);
        hsTabs.post(() -> hsTabs.smoothScrollTo(
                Math.max(0, (tab.getLeft() + tab.getRight()) / 2 - hsTabs.getWidth() / 2), 0));

        productList.clear();
        productList.addAll(filterByTab(TAB_LABELS[index]));
        shopAdapter.notifyDataSetChanged();
        rvShop.scrollToPosition(0);
        if (nsvShop != null) {
            nsvShop.scrollTo(0, 0);
        }
    }

    private List<ShopItem> filterByTab(String tab) {
        List<ShopItem> result = new ArrayList<>();
        if ("闪购".equals(tab)) {
            result.addAll(allItems);
            Collections.reverse(result);
            return result;
        }
        for (ShopItem item : allItems) {
            if (matchesTab(tab, item.getCategory())) {
                result.add(item);
            }
        }
        return result.isEmpty() ? new ArrayList<>(allItems) : result;
    }

    private boolean matchesTab(String tab, String category) {
        switch (tab) {
            case "关注": return category.equals("食品") || category.equals("家居");
            case "国补": return category.equals("数码");
            case "飞猪": return category.equals("服饰") || category.equals("家居");
            case "新风潮": return category.equals("数码") || category.equals("美妆");
            case "穿搭": return category.equals("服饰") || category.equals("美妆");
            default:     return true; // 推荐 -> 全部
        }
    }

    private static class ShopEntry {
        final int iconRes;
        final String label;
        ShopEntry(int iconRes, String label) {
            this.iconRes = iconRes;
            this.label = label;
        }
    }

    private List<List<ShopEntry>> buildEntryPages() {
        List<List<ShopEntry>> pages = new ArrayList<>();

        List<ShopEntry> page1 = new ArrayList<>();
        page1.add(new ShopEntry(R.drawable.shop_entry_01, "百亿补贴"));
        page1.add(new ShopEntry(R.drawable.shop_entry_02, "淘宝秒杀"));
        page1.add(new ShopEntry(R.drawable.shop_entry_03, "淘宝直播"));
        page1.add(new ShopEntry(R.drawable.shop_entry_04, "充值中心"));
        page1.add(new ShopEntry(R.drawable.shop_entry_05, "新人特惠"));
        page1.add(new ShopEntry(R.drawable.shop_entry_06, "红包签到"));
        page1.add(new ShopEntry(R.drawable.shop_entry_07, "芭芭农场"));
        page1.add(new ShopEntry(R.drawable.shop_entry_08, "领淘金币"));
        page1.add(new ShopEntry(R.drawable.shop_entry_09, "阿里拍卖"));
        page1.add(new ShopEntry(R.drawable.shop_entry_10, "天猫超市"));
        page1.add(new ShopEntry(R.drawable.shop_entry_11, "天猫国际"));
        page1.add(new ShopEntry(R.drawable.shop_entry_12, "今日特卖"));
        page1.add(new ShopEntry(R.drawable.shop_entry_13, "天天特卖"));
        page1.add(new ShopEntry(R.drawable.shop_entry_14, "闲鱼"));
        page1.add(new ShopEntry(R.drawable.shop_entry_15, "飞猪旅行"));
        pages.add(page1);

        List<ShopEntry> page2 = new ArrayList<>();
        page2.add(new ShopEntry(R.drawable.shop_entry_15, "飞猪旅行"));
        page2.add(new ShopEntry(R.drawable.shop_entry_16, "阿里药房"));
        page2.add(new ShopEntry(R.drawable.shop_entry_17, "天猫新品"));
        page2.add(new ShopEntry(R.drawable.shop_entry_18, "阿里拍卖"));
        page2.add(new ShopEntry(R.drawable.shop_entry_19, "淘宝礼物"));
        page2.add(new ShopEntry(R.drawable.shop_entry_20, "有好券"));
        page2.add(new ShopEntry(R.drawable.shop_entry_21, "天猫国际"));
        page2.add(new ShopEntry(R.drawable.shop_entry_22, "分类"));
        page2.add(new ShopEntry(R.drawable.shop_entry_23, "淘宝闪购"));
        page2.add(new ShopEntry(R.drawable.shop_entry_24, "资质规则"));
        page2.add(new ShopEntry(R.drawable.shop_entry_25, "全部频道"));
        pages.add(page2);
        return pages;
    }

    private void setupEntryPager() {
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvEntries.setLayoutManager(layoutManager);
        rvEntries.setAdapter(new EntryPageAdapter(buildEntryPages()));

        new PagerSnapHelper().attachToRecyclerView(rvEntries);
        rvEntries.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateEntryDots();
            }
        });
        updateEntryDots();
        fixEntryPagerHeight();
    }

    private void fixEntryPagerHeight() {
        rvEntries.post(() -> {
            int width = rvEntries.getWidth();
            if (width == 0) return;
            GridLayout probe = (GridLayout) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_shop_page, rvEntries, false);
            for (int i = 0; i < 15; i++) {
                probe.addView(LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_shop_entry, probe, false));
            }
            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            probe.measure(widthSpec, heightSpec);
            ViewGroup.LayoutParams lp = rvEntries.getLayoutParams();
            lp.height = probe.getMeasuredHeight();
            rvEntries.setLayoutParams(lp);
        });
    }

    private void updateEntryDots() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvEntries.getLayoutManager();
        int page = 0;
        if (layoutManager != null) {
            int first = layoutManager.findFirstVisibleItemPosition();
            View firstView = layoutManager.findViewByPosition(Math.max(first, 0));
            if (firstView != null && firstView.getWidth() > 0
                    && -firstView.getX() > firstView.getWidth() / 2f) {
                page = first + 1;
            } else if (first >= 0) {
                page = first;
            }
        }
        for (int i = 0; i < llDots.getChildCount(); i++) {
            llDots.getChildAt(i).setBackgroundResource(i == page
                    ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        }
    }

    private void handleEntryClick(ShopEntry entry) {
        String name = entry.label;
        String pageName;
        switch (name) {
            case "百亿补贴":
                pageName = "subsidy.html";
                break;
            case "淘宝秒杀":
                pageName = "seckill.html";
                break;
            case "淘宝直播":
                pageName = "live.html";
                break;
            case "充值中心":
                pageName = "recharge.html";
                break;
            case "新人特惠":
                pageName = "newuser.html";
                break;
            case "红包签到":
            case "领淘金币":
                pageName = "signin.html";
                break;
            case "芭芭农场":
                pageName = "farm.html";
                break;
            case "阿里拍卖":
                pageName = "auction.html";
                break;
            case "天猫超市":
                pageName = "supermarket.html";
                break;
            case "天猫国际":
                pageName = "global.html";
                break;
            case "今日特卖":
            case "天天特卖":
                pageName = "temai.html";
                break;
            case "闲鱼":
                pageName = "xianyu.html";
                break;
            case "飞猪旅行":
                pageName = "feizhu.html";
                break;
            case "阿里药房":
                pageName = "pharmacy.html";
                break;
            case "天猫新品":
                pageName = "newproducts.html";
                break;
            case "淘宝礼物":
                pageName = "gifts.html";
                break;
            case "有好券":
                pageName = "coupons.html";
                break;
            case "分类":
            case "全部频道":
                pageName = "categories.html";
                break;
            case "淘宝闪购":
                pageName = "flashsale.html";
                break;
            case "资质规则":
                pageName = "rules.html";
                break;
            default:
                pageName = "subsidy.html";
                break;
        }
        WebActivity.open(requireContext(), name, "file:///android_asset/web/" + pageName);
    }

    private class EntryPageAdapter extends RecyclerView.Adapter<EntryPageAdapter.PageViewHolder> {

        private final List<List<ShopEntry>> pages;

        EntryPageAdapter(List<List<ShopEntry>> pages) {
            this.pages = pages;
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            final GridLayout grid;
            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                grid = (GridLayout) itemView;
            }
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PageViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_shop_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.grid.removeAllViews();
            for (final ShopEntry entry : pages.get(position)) {
                View cell = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_shop_entry, holder.grid, false);
                ((ImageView) cell.findViewById(R.id.iv_entry_icon)).setImageResource(entry.iconRes);
                ((TextView) cell.findViewById(R.id.tv_entry_label)).setText(entry.label);
                cell.setOnClickListener(v -> handleEntryClick(entry));
                holder.grid.addView(cell);
            }
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    private void bindEvent() {
        View btnCart = findViewById(R.id.btn_cart);
        if (btnCart != null) {
            btnCart.setOnClickListener(v -> ShoppingDialogs.showCart(requireContext()));
        }

        View tvShopFooter = findViewById(R.id.tv_shop_footer);
        if (tvShopFooter != null) {
            tvShopFooter.setOnClickListener(v ->
                    WebActivity.open(requireContext(), "领券中心", "file:///android_asset/web/coupons.html"));
        }

        TextView tvSearchHint = findViewById(R.id.tv_search_hint);
        tvSearchHint.setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(requireContext());
            input.setHint("商品名称或分类");
            new android.app.AlertDialog.Builder(requireContext()).setTitle("搜索商品").setView(input)
                    .setPositiveButton("搜索",(d,w)->{
                        String key=input.getText().toString().trim();
                        productList.clear();
                        for(ShopItem item:allItems) if((item.getTitle()+item.getCategory()).contains(key)) productList.add(item);
                        shopAdapter.notifyDataSetChanged();
                        tvSearchHint.setText(key.isEmpty()?"搜索你喜欢的商品":"搜索："+key+"（"+productList.size()+"件）");
                        if(productList.isEmpty()) Toast.makeText(requireContext(),"没有匹配商品，换个关键词试试",Toast.LENGTH_LONG).show();
                    }).setNeutralButton("查看全部",(d,w)->{selectTab(DEFAULT_TAB); tvSearchHint.setText("搜索你喜欢的商品");})
                    .setNegativeButton("取消",null).show();
        });
    }
}
