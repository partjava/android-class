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

    private static final String[] TAB_LABELS = {"推荐", "数码", "家具", "穿搭", "水果", "个护"};
    private static final int DEFAULT_TAB = 0; // 默认选中“推荐”

    private HorizontalScrollView hsTabs;
    private LinearLayout llTabs, llDots;
    private RecyclerView rvEntries, rvShop;
    private androidx.core.widget.NestedScrollView nsvShop;

    private final List<View> tabViews = new ArrayList<>();
    private int currentTab = DEFAULT_TAB;

    private List<ShopItem> allItems;    //全部商品
    private List<ShopItem> productList; //当前频道的商品，ShopAdapter 持有它的引用（同 HomeActivity 的 newsList）
    private ShopAdapter shopAdapter;
    private TextView goodsStatus;
    private int goodsRequest;
    private static volatile List<ShopItem> serverCatalog = Collections.emptyList();

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
        goodsStatus = findViewById(R.id.tv_goods_status);
        goodsStatus.setOnClickListener(v -> loadGoodsFromNetwork());
        nsvShop = findViewById(R.id.nsv_shop);

        View tvFooter = findViewById(R.id.tv_shop_footer);
        if (tvFooter instanceof TextView) {
            ((TextView) tvFooter).setText("🔄 点击换一批网易严选好物 (30件/批) 🛍️");
            tvFooter.setOnClickListener(v -> loadGoodsFromNetwork());
        }
    }

    private void initShopData() {
        allItems = new ArrayList<>();

        productList = new ArrayList<>();
        shopAdapter = new ShopAdapter(productList);

        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvShop.setLayoutManager(layoutManager);
        rvShop.setAdapter(shopAdapter);

        shopAdapter.setOnItemClickListener(item ->
                startActivity(ProductDetailActivity.intent(requireContext(), item)));
    }

    private void loadGoodsFromNetwork() {
        loadGoodsFromNetwork(TAB_LABELS[currentTab], true);
    }

    private void loadGoodsFromNetwork(String category) {
        loadGoodsFromNetwork(category, false);
    }

    private void loadGoodsFromNetwork(String category, boolean refresh) {
        final int request = ++goodsRequest;
        allItems.clear(); serverCatalog = Collections.emptyList();
        productList.clear(); shopAdapter.notifyDataSetChanged();
        goodsStatus.setText(refresh ? "正在实时换一批严选好物…" : "正在加载商品…");
        goodsStatus.setVisibility(View.VISIBLE);
        goodsStatus.setClickable(false);
        String url;
        try {
            url = CampusNetworkConfig.getApiBaseUrl() + "/goods?category=" + java.net.URLEncoder.encode(category, "UTF-8")
                    + "&limit=30" + (refresh ? "&refresh=true" : "");
        } catch (java.io.UnsupportedEncodingException e) {
            url = CampusNetworkConfig.getApiBaseUrl() + "/goods?limit=30" + (refresh ? "&refresh=true" : "");
        }
        ApiClient.get(url, new ApiClient.Callback() {
            private boolean current() { return isAdded() && getView() != null && request == goodsRequest; }
            @Override public void onSuccess(org.json.JSONObject response) {
                if (!current()) return;
                try {
                    if (response.optInt("code") != 200) throw new IllegalArgumentException("服务返回错误");
                    org.json.JSONArray items = response.getJSONObject("data").getJSONArray("items");
                    List<ShopItem> netList = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) {
                        org.json.JSONObject obj = items.getJSONObject(i);
                        String pid = obj.getString("product_id"), title = obj.getString("title");
                        double price = obj.getDouble("price");
                        if (pid.isEmpty() || title.trim().isEmpty() || !Double.isFinite(price) || price < 0) throw new IllegalArgumentException("商品数据无效");
                        netList.add(new ShopItem(pid, title, price, obj.optInt("sales", 0), R.drawable.ic_shop,
                                obj.optString("category", "其他"), obj.optString("image_url", ""),
                                obj.optJSONObject("details") == null ? obj.toString() : obj.getJSONObject("details").toString()));
                    }
                    allItems = netList; serverCatalog = new ArrayList<>(netList);
                    productList.clear();
                    productList.addAll(netList);
                    shopAdapter.notifyDataSetChanged();
                    goodsStatus.setText(netList.isEmpty() ? "暂无商品 · 点击刷新" : "🔄 网易严选官网直发 · 实时换一批 (30件/批)");
                    goodsStatus.setClickable(true);
                    if (refresh && isAdded()) {
                        Toast.makeText(requireContext(), "已为您实时刷新 30 件网易严选好物", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception error) { onError("商品数据格式错误"); }
            }
            @Override public void onError(String message) {
                if (!current()) return;
                goodsStatus.setText("商品加载失败，请检查后端连接 · 点击重试");
                goodsStatus.setClickable(true);
            }
        });
    }

    @Override public void onDestroyView() {
        ++goodsRequest;
        tabViews.clear();
        super.onDestroyView();
    }

    /**
     * 全部商品的目录数据。抽成 static 是因为商品详情页的
     * 「你可能还会喜欢」也要拿同一份目录来挑推荐位——
     * 详情页拿到的是目录里的同一个 ShopItem 实例（走 Intent 序列化
     * 会在另一侧还原出内容相等的对象，引用比较在详情页内不做）。
     */
    static List<ShopItem> buildCatalog() { return new ArrayList<>(serverCatalog); }

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

        loadGoodsFromNetwork(TAB_LABELS[index]);
        rvShop.scrollToPosition(0);
        if (nsvShop != null) {
            nsvShop.scrollTo(0, 0);
        }
    }

    private List<ShopItem> filterByTab(String tab) {
        List<ShopItem> result = new ArrayList<>();
        if ("推荐".equals(tab) || "全部".equals(tab)) {
            return new ArrayList<>(allItems);
        }
        for (ShopItem item : allItems) {
            if (matchesTab(tab, item.getCategory())) {
                result.add(item);
            }
        }
        return result.isEmpty() ? new ArrayList<>(allItems) : result;
    }

    private boolean matchesTab(String tab, String category) {
        if ("推荐".equals(tab) || "全部".equals(tab)) {
            return true;
        }
        return tab != null && tab.equals(category);
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
