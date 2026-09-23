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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 商城页，对照淘宝首页做的东西：
 *
 * · 顶部频道标签：HorizontalScrollView 一排 tab，放不下可左右滑，
 *   选中带红下划线，并按商品分类过滤瀑布流
 * · 宫格入口：横向 RecyclerView + PagerSnapHelper，一页一个 GridLayout，
 *   松手自动吸附到整页——淘宝式左右翻页的手感就是这么来的；
 *   图标是从淘宝截图裁的位图（drawable-nodpi/shop_entry_01~25.png）
 * · 商品瀑布流：RecyclerView + StaggeredGridLayoutManager，
 *   两列各自独立往下排，高度由图片比例决定
 */
public class ShopActivity extends AppCompatActivity {

    //频道标签，选中的会过滤下方瀑布流；"推荐"显示全部
    private static final String[] TAB_LABELS = {"关注", "推荐", "闪购", "国补", "飞猪", "新风潮", "穿搭"};
    private static final int DEFAULT_TAB = 1;//默认选中"推荐"

    private HorizontalScrollView hsTabs;
    private LinearLayout llTabs, llDots;
    private RecyclerView rvEntries, rvShop;
    private BottomNavigationView bottomNav;

    private final List<View> tabViews = new ArrayList<>();
    private int currentTab = DEFAULT_TAB;

    private List<ShopItem> allItems;    //全部商品
    private List<ShopItem> productList; //当前频道的商品，ShopAdapter 持有它的引用（同 HomeActivity 的 newsList）
    private ShopAdapter shopAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
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
        bottomNav = findViewById(R.id.bottom_nav);
    }

    //初始化商品数据：12 个商品，图片高度各不相同（300~560px），保证错落感
    private void initShopData() {
        allItems = new ArrayList<>();
        allItems.add(new ShopItem("无线蓝牙耳机 半入耳式 超长续航 通话降噪", 129.00, 8632, R.drawable.shop_1, "数码"));
        allItems.add(new ShopItem("316不锈钢保温杯 大容量男女便携水杯", 39.90, 24310, R.drawable.shop_2, "家居"));
        allItems.add(new ShopItem("静音机械键盘 87键 青轴 背光游戏键盘", 219.00, 1531, R.drawable.shop_3, "数码"));
        allItems.add(new ShopItem("香薰蜡烛持久助眠 家用室内熏香礼盒装", 49.90, 418, R.drawable.shop_4, "家居"));
        allItems.add(new ShopItem("纯棉四件套床上用品 全棉贡缎床单款", 189.00, 7612, R.drawable.shop_5, "家居"));
        allItems.add(new ShopItem("智能手表 运动手环 心率睡眠监测", 299.00, 11586, R.drawable.shop_6, "数码"));
        allItems.add(new ShopItem("零食大礼包整箱 网红小吃休闲食品", 59.80, 3821, R.drawable.shop_7, "食品"));
        allItems.add(new ShopItem("北欧简约台灯 卧室床头灯LED护眼", 89.00, 954, R.drawable.shop_8, "家居"));
        allItems.add(new ShopItem("多功能收纳盒抽屉式 整理箱衣物储物", 25.90, 6534, R.drawable.shop_9, "家居"));
        allItems.add(new ShopItem("轻奢双肩包男女同款 商务电脑背包", 159.00, 2078, R.drawable.shop_10, "服饰"));
        allItems.add(new ShopItem("挂脖风扇usb充电 静音便携无叶风扇", 69.90, 3296, R.drawable.shop_11, "数码"));
        allItems.add(new ShopItem("陶瓷马克杯创意咖啡杯 带盖勺礼盒装", 35.00, 1476, R.drawable.shop_12, "家居"));

        productList = new ArrayList<>();
        shopAdapter = new ShopAdapter(productList);
        //两列、纵向滚动。item 高度不一时，两列自然错落开
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvShop.setLayoutManager(layoutManager);
        rvShop.setAdapter(shopAdapter);

        shopAdapter.setOnItemClickListener(item ->
                Toast.makeText(ShopActivity.this,
                        "点击了：" + item.getTitle(), Toast.LENGTH_SHORT).show());
    }

    //==== 频道标签：可左右滑动的那一排 ====

    private void setupTabs() {
        LayoutInflater inflater = LayoutInflater.from(this);
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

    //选中一个频道：高亮标签 + 把标签滚到屏幕中间 + 换瀑布流数据
    private void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < tabViews.size(); i++) {
            boolean selected = i == index;
            TextView tv = tabViews.get(i).findViewById(R.id.tv_tab_label);
            View line = tabViews.get(i).findViewById(R.id.v_tab_line);
            tv.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.brand_red : R.color.text_primary));
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            line.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
        //7 个标签一屏放不下，把选中的滚到可视区域中间。
        //post() 是等测量完成后宽度才有值（onCreate 里还没有布局）
        View tab = tabViews.get(index);
        hsTabs.post(() -> hsTabs.smoothScrollTo(
                Math.max(0, (tab.getLeft() + tab.getRight()) / 2 - hsTabs.getWidth() / 2), 0));
        //换数据：clear + addAll，adapter 持有同一个引用（和首页切换新闻标签同一套路）
        productList.clear();
        productList.addAll(filterByTab(TAB_LABELS[index]));
        shopAdapter.notifyDataSetChanged();
        rvShop.scrollToPosition(0);
    }

    //按频道过滤商品；没对应分类的频道兜底显示全部，避免空白页
    private List<ShopItem> filterByTab(String tab) {
        List<ShopItem> result = new ArrayList<>();
        if ("闪购".equals(tab)) {
            //限时抢购：全部商品倒个顺序，营造"换了一批"的效果
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

    /**
     * 频道 → 商品分类的映射。
     *
     * 7 个频道是从淘宝抄来的名字，而演示数据只有 4 个分类
     * （家居 6 个、数码 4 个、食品 1 个、服饰 1 个），两边对不上，
     * 所以这里的目标不是语义精确，而是**别让哪个频道点开来是空的**。
     *
     * 原来「飞猪 / 新风潮 / 穿搭」三个频道共用一个 return，全指向「服饰」，
     * 而服饰只有 1 个商品——点进去就是一个孤零零的双肩包。
     * filterByTab() 的兜底只在结果**为空**时才生效，一条不算空，兜底救不了。
     *
     * 现在每个频道至少 4 个商品，且五组集合互不相同（分类只有 4 个，
     * 频道有 5 个，重叠不可避免，但不再有两组完全一样）。
     */
    private boolean matchesTab(String tab, String category) {
        switch (tab) {
            //7 个：食品 + 家居
            case "关注": return category.equals("食品") || category.equals("家居");
            //4 个：数码
            case "国补": return category.equals("数码");
            //7 个：服饰 + 家居。双肩包、保温杯、四件套这些当出行用品说得通
            case "飞猪": return category.equals("服饰") || category.equals("家居");
            //5 个：数码 + 食品
            case "新风潮": return category.equals("数码") || category.equals("食品");
            //5 个：服饰 + 数码。智能手表、无线耳机、挂脖风扇本来就是穿搭配饰
            case "穿搭": return category.equals("服饰") || category.equals("数码");
            default:     return true;//推荐 → 全部
        }
    }

    //==== 宫格入口：可左右翻页的两页 ====

    //宫格入口的数据模型：图标 + 文字
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
        //第一页：2 行 x 5 列
        List<ShopEntry> page1 = new ArrayList<>();
        page1.add(new ShopEntry(R.drawable.shop_entry_01, "百亿补贴"));
        page1.add(new ShopEntry(R.drawable.shop_entry_02, "淘宝秒杀"));
        page1.add(new ShopEntry(R.drawable.shop_entry_03, "淘宝直播"));
        page1.add(new ShopEntry(R.drawable.shop_entry_04, "充值中心"));
        page1.add(new ShopEntry(R.drawable.shop_entry_05, "新人特惠"));
        page1.add(new ShopEntry(R.drawable.shop_entry_06, "红包签到"));
        page1.add(new ShopEntry(R.drawable.shop_entry_07, "芭芭农场"));
        page1.add(new ShopEntry(R.drawable.shop_entry_08, "领淘金币"));
        page1.add(new ShopEntry(R.drawable.shop_entry_09, "天猫超市"));
        page1.add(new ShopEntry(R.drawable.shop_entry_10, "菜鸟驿站"));
        pages.add(page1);
        //第二页：3 行 x 5 列
        List<ShopEntry> page2 = new ArrayList<>();
        page2.add(new ShopEntry(R.drawable.shop_entry_11, "聚划算"));
        page2.add(new ShopEntry(R.drawable.shop_entry_12, "常买超市"));
        page2.add(new ShopEntry(R.drawable.shop_entry_13, "试用领取"));
        page2.add(new ShopEntry(R.drawable.shop_entry_14, "活动日历"));
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
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvEntries.setLayoutManager(layoutManager);
        rvEntries.setAdapter(new EntryPageAdapter(buildEntryPages()));
        //PagerSnapHelper：一页一屏，松手自动吸附到最近的一页
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

    //两页行数不同（第一页 2 行、第二页 3 行），而 pager 是 wrap_content：
    //初始高度只会按先显示的第一页量成 2 行，翻到第二页时第三行就被裁掉了。
    //所以这里离屏造一页 15 格的探针，量出 3 行的真实高度，
    //把 pager 高度定死成这个值——两页共用，谁也不会被裁。
    private void fixEntryPagerHeight() {
        rvEntries.post(() -> {
            int width = rvEntries.getWidth();
            if (width == 0) return;
            GridLayout probe = (GridLayout) LayoutInflater.from(this)
                    .inflate(R.layout.item_shop_page, rvEntries, false);
            for (int i = 0; i < 15; i++) {
                probe.addView(LayoutInflater.from(this)
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

    //滑动过程中实时判断停在第几页：第一页被滑出去超过一半宽度就算翻到第二页
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

    //宫格入口的翻页适配器：一个 item 就是一页 GridLayout，
    //页里的格子再逐个 inflate 塞进去
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
            holder.grid.removeAllViews();//复用时先清掉旧格子
            for (final ShopEntry entry : pages.get(position)) {
                View cell = LayoutInflater.from(ShopActivity.this)
                        .inflate(R.layout.item_shop_entry, holder.grid, false);
                ((ImageView) cell.findViewById(R.id.iv_entry_icon)).setImageResource(entry.iconRes);
                ((TextView) cell.findViewById(R.id.tv_entry_label)).setText(entry.label);
                cell.setOnClickListener(v ->
                        Toast.makeText(ShopActivity.this, entry.label, Toast.LENGTH_SHORT).show());
                holder.grid.addView(cell);
            }
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    //==== 搜索入口 + 底部导航 ====

    private void bindEvent() {
        //顶栏搜索入口只是个占位，不做真正的搜索
        TextView tvSearchHint = findViewById(R.id.tv_search_hint);
        tvSearchHint.setOnClickListener(v ->
                Toast.makeText(ShopActivity.this, "搜索功能仅为演示", Toast.LENGTH_SHORT).show());

        //==== 底部导航：写法和首页/我的页保持一致 ====
        bottomNav.setSelectedItemId(R.id.nav_shop);
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    //复用栈里已有的首页；CLEAR_TOP 会把商城自己清出返回栈
                    Intent intent = new Intent(ShopActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else if (itemId == R.id.nav_video) {
                    //跳转到视频页，和跳首页/我的用同一套栈管理策略
                    Intent intent = new Intent(ShopActivity.this, VideoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else if (itemId == R.id.nav_add) {
                    Toast.makeText(ShopActivity.this, "点击发布", Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.nav_shop) {
                    Toast.makeText(ShopActivity.this, "当前在商城", Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.nav_mine) {
                    //跳转到我的页面，和首页跳"我的"用同一套栈管理策略
                    Intent intent = new Intent(ShopActivity.this, MineActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                return true;
            }
        });
    }
}
