package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.app.Dialog;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 商品详情页。
 *
 * 参照抖音商城详情页的版式，从上到下：图片轮播 → 锚点 tab 行 →
 * 价格卡 / 标题卡 / 服务卡 / 评价卡 / 店铺卡 / 商品详情卡 / 推荐流 →
 * 底部操作栏（进店 / 客服 / 购物车角标 / 加入购物车 / 立即购买）。
 *
 * 数据全部来自 Intent 里的那一个 ShopItem + 按品类映射的静态文案表；
 * 下单链路完全复用现有设施：加购走 ShopStore.addToCart，直接购买走
 * OrderConfirmActivity 的 EXTRA_DIRECT_* extras。
 */
public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM = "extra_shop_item";

    //SKU 弹窗的两种模式：确认键的文案和颜色随它切换
    private static final int MODE_ADD = 0;
    private static final int MODE_BUY = 1;

    //画廊固定 3 页：1 页实拍图 + 2 页营销海报（每商品只有一张实拍图，
    //海报页用纯色底 + 卖点文案补位，避免拿别家商品的照片冒充）
    private static final int GALLERY_PAGE_COUNT = 3;

    //===== 按品类映射的内容表（顺序：数码 / 家居 / 食品 / 服饰 / 美妆）=====
    private static final String[] CATEGORIES = {"数码", "家居", "食品", "服饰", "美妆"};
    private static final String[] STORE_NAMES = {
            "臻品数码专营店", "洁丽居家日用旗舰店", "味觉严选食品店", "云衫服饰旗舰店", "花漾美妆专营店"};
    private static final String[] ORIGINS = {
            "广东深圳", "浙江宁波", "四川成都", "广东广州", "上海"};
    private static final String[][] SPECS = {
            {"标准版", "旗舰版", "礼盒装"},
            {"基础款", "加大款", "礼盒装"},
            {"单袋装", "3袋组合", "整箱装"},
            {"M码", "L码", "XL码"},
            {"经典香型", "清新香型", "限定香型"},
    };
    private static final String[] POSTER_TITLES = {
            "科技体验派", "舒居生活家", "严选好味道", "轻尚穿搭志", "焕颜研究所"};
    private static final String[] POSTER_CAPTIONS = {
            "旗舰配置 · 极致体验\n全场景智能互联",
            "天然材质 · 温润质感\n把家过成想要的样子",
            "新鲜原料 · 匠心制作\n每一口都吃得安心",
            "精选面料 · 舒适剪裁\n百搭出行的每一天",
            "植萃配方 · 温和亲肤\n养出透亮好状态",
    };
    private static final String[][] DESC_PARAGRAPHS = {
            {"产品采用新一代核心方案，配合精细调校的系统调度，性能释放稳定持久。无论是日常通勤还是重度使用，都能保持流畅体验。",
             "出厂前经过 32 项严苛可靠性测试，包括高低温循环、跌落冲击与连续老化，确保每一件产品都经得起日常使用的考验。",
             "包装内含标配配件与快速上手指南，官方渠道购买享受一年质保与全国联保服务，售后无忧。"},
            {"选用环保级原材料，表面处理工艺细腻，触感温润。设计上兼顾实用与美感，轻松融入各类家居风格。",
             "结构稳固耐用，关键部位加强处理，承重与耐用性经过实验室反复验证，日常使用更放心。",
             "本品支持七天无理由退换，物流全程可追溯，大件商品提供送货上楼服务，收货更省心。"},
            {"原料来自核心产区，批次新鲜可追溯。坚持不添加人工香精与合成色素，保留食材本来的味道。",
             "生产车间通过食品安全管理体系认证，从原料入库到成品出库全程恒温控制，锁住新鲜。",
             "独立密封包装，防潮防氧化。开袋即食，也适合与家人朋友分享，办公室囤货同样合适。"},
            {"面料经过多次水洗预缩处理，版型挺括不易变形。剪裁贴合人体工学，上身舒适不紧绷。",
             "细节处见品质：走线均匀密实，纽扣与拉链均通过上万次开合测试，耐穿耐洗。",
             "提供多个尺码选择，支持七天无理由退换与免费换码，网购也能合身。"},
            {"核心成分采用植物萃取配方，温和低刺激，敏感肌也适用。质地清爽不黏腻，吸收快。",
             "通过皮肤敏感性测试，配方不含酒精。使用前建议在耳后做一次皮试，更安心。",
             "建议置于阴凉干燥处保存，避免阳光直射。开封后 12 个月内使用完毕效果最佳。"},
    };

    //评价池：8 条固定文案，按标题 hash 决定每件商品展示哪两条，
    //同一件商品每次进来看到的都一样（确定性伪随机）
    private static final String[] REVIEW_NAMES = {
            "d**8", "回**0", "小**鱼", "风**过", "星**河", "橘**子", "木**南", "清**欢"};
    private static final String[] REVIEW_TEXTS = {
            "宝贝收到了，很满意的一次购物，包装严实，质量比想象中还好，下次还会回购！",
            "不错，挺香的，物流也快，客服态度很好，值得推荐给身边的朋友。",
            "性价比很高，用了一段时间才来评价，整体没有让人失望，好评。",
            "第二次购买了，品质一直很稳定，家里人都说好用，放心囤。",
            "做工精细，细节到位，和商品页描述一致，没有色差，满意。",
            "发货速度很快，第二天就到了，包装很好，体验超出预期。",
            "朋友推荐来买的，果然没让我失望，已经推荐给同事了。",
            "整体不错，就是快递稍微慢了半天，不过商品本身没得挑，好评。",
    };
    //头像彩底直接取会话列表那套色板（avatar_blue/green/orange/purple）
    private static final int[] AVATAR_COLORS = {
            R.color.avatar_blue, R.color.avatar_green, R.color.avatar_orange, R.color.avatar_purple};

    private ShopItem item;
    private ShopStore store;
    private int catIndex;
    private String[] specs;
    private int specIndex;
    private int skuQty = 1;

    private NestedScrollView nsv;
    private ViewPager2 vpGallery;
    private ImageView ivGalleryPlay;
    private TextView tvGalleryIndex, tvCartBadge;
    private View[] tabCells, tabLines;
    private LinearLayout llPriceCard, llReviewCard, llDescCard, llRecommendCard;
    private TextView tvPriceMain, tvPriceOrigin, tvStock, tvPriceSales;
    private TextView tvDetailTitle;
    private TextView[] tagViews = new TextView[3];
    private TextView tvServiceShip, tvServiceDelivery, tvServiceSpec;
    private TextView tvReviewHeader, tvReviewRate;
    private LinearLayout llReviewRows;
    private TextView tvStoreAvatar, tvStoreName, tvStoreScore;
    private TextView tvDescBody1, tvDescBody2, tvDescBody3;
    private LinearLayout llDescParams;
    private RecyclerView rvRecommend;

    /** 统一入口：详情页的 ShopItem 整体走 Serializable extra 传进来 */
    public static Intent intent(Context context, ShopItem item) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra(EXTRA_ITEM, item);
        return intent;
    }

    public static void open(Context context, ShopItem item) {
        Intent launch = intent(context, item);
        if (!(context instanceof android.app.Activity)) launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        //防御：extra 丢失（比如进程被杀后重建）直接退出，不能裸奔空指针
        item = (ShopItem) getIntent().getSerializableExtra(EXTRA_ITEM);
        if (item == null) {
            Toast.makeText(this, "商品信息加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        store = new ShopStore(this);
        catIndex = catIndexOf(item.getCategory());
        specs = SPECS[catIndex];

        bindViews();
        setupGallery();
        setupTabs();
        renderPriceCard();
        renderTitleCard();
        renderServicesCard();
        renderReviewsCard();
        renderShopCard();
        renderDetailSection();
        setupRecommendations();
        setupBottomBar();
        refreshCartBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //从购物车页返回时数量可能变了；onCreate 只管第一次
        refreshCartBadge();
    }

    private void bindViews() {
        nsv = findViewById(R.id.nsv_detail);
        vpGallery = findViewById(R.id.vp_gallery);
        ivGalleryPlay = findViewById(R.id.iv_gallery_play);
        tvGalleryIndex = findViewById(R.id.tv_gallery_index);
        tvCartBadge = findViewById(R.id.tv_cart_badge);
        tabCells = new View[]{findViewById(R.id.ll_tab_product), findViewById(R.id.ll_tab_review),
                findViewById(R.id.ll_tab_desc), findViewById(R.id.ll_tab_recommend)};
        tabLines = new View[]{findViewById(R.id.v_tab_product_line), findViewById(R.id.v_tab_review_line),
                findViewById(R.id.v_tab_desc_line), findViewById(R.id.v_tab_recommend_line)};
        llPriceCard = findViewById(R.id.ll_price_card);
        llReviewCard = findViewById(R.id.ll_review_card);
        llDescCard = findViewById(R.id.ll_desc_card);
        llRecommendCard = findViewById(R.id.ll_recommend_card);
        tvPriceMain = findViewById(R.id.tv_price_main);
        tvPriceOrigin = findViewById(R.id.tv_price_origin);
        tvStock = findViewById(R.id.tv_stock);
        tvPriceSales = findViewById(R.id.tv_price_sales);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tagViews[0] = findViewById(R.id.tv_tag_1);
        tagViews[1] = findViewById(R.id.tv_tag_2);
        tagViews[2] = findViewById(R.id.tv_tag_3);
        tvServiceShip = findViewById(R.id.tv_service_ship);
        tvServiceDelivery = findViewById(R.id.tv_service_delivery);
        tvServiceSpec = findViewById(R.id.tv_service_spec);
        tvReviewHeader = findViewById(R.id.tv_review_header);
        tvReviewRate = findViewById(R.id.tv_review_rate);
        llReviewRows = findViewById(R.id.ll_review_rows);
        tvStoreAvatar = findViewById(R.id.tv_store_avatar);
        tvStoreName = findViewById(R.id.tv_store_name);
        tvStoreScore = findViewById(R.id.tv_store_score);
        tvDescBody1 = findViewById(R.id.tv_desc_body1);
        tvDescBody2 = findViewById(R.id.tv_desc_body2);
        tvDescBody3 = findViewById(R.id.tv_desc_body3);
        llDescParams = findViewById(R.id.ll_desc_params);
        rvRecommend = findViewById(R.id.rv_recommend);
    }

    //===== 画廊 =====

    private void setupGallery() {
        vpGallery.setAdapter(new GalleryAdapter());
        //页码角标 + 装饰播放键（播放键只属于第 1 页的「视频封面」语境）
        vpGallery.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvGalleryIndex.setText((position + 1) + "/" + GALLERY_PAGE_COUNT);
                ivGalleryPlay.setVisibility(position == 0 ? View.VISIBLE : View.INVISIBLE);
            }
        });
        tvGalleryIndex.setText("1/" + GALLERY_PAGE_COUNT);

        findViewById(R.id.btn_gallery_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_gallery_share).setOnClickListener(v ->
                toast("演示模式：分享功能暂未接入"));
        findViewById(R.id.btn_gallery_more).setOnClickListener(v ->
                toast("演示模式：更多操作暂未接入"));
    }

    /** 画廊页适配器：2 种页型（实拍图 / 营销海报） */
    private class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_PHOTO = 0;
        private static final int TYPE_POSTER = 1;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_PHOTO) {
                //⚠️ attachToRoot 必须为 false，页面尺寸由 ViewPager2 决定
                return new PhotoHolder(inflater.inflate(R.layout.item_gallery_photo, parent, false));
            }
            return new PosterHolder(inflater.inflate(R.layout.item_gallery_poster, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof PhotoHolder) {
                ((PhotoHolder) holder).iv.setImageResource(item.getImgRes());
            } else {
                bindPoster((PosterHolder) holder, position);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_PHOTO : TYPE_POSTER;
        }

        @Override
        public int getItemCount() {
            return GALLERY_PAGE_COUNT;
        }
    }

    private static class PhotoHolder extends RecyclerView.ViewHolder {
        final ImageView iv;
        PhotoHolder(@NonNull View itemView) {
            super(itemView);
            iv = (ImageView) itemView;
        }
    }

    private static class PosterHolder extends RecyclerView.ViewHolder {
        final View root;
        final TextView title, caption, footer;
        PosterHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            title = itemView.findViewById(R.id.tv_poster_title);
            caption = itemView.findViewById(R.id.tv_poster_caption);
            footer = itemView.findViewById(R.id.tv_poster_footer);
        }
    }

    /** 海报页：第 2 页红系、第 3 页黄系，两套配色都取自现成 token */
    private void bindPoster(PosterHolder holder, int position) {
        boolean warm = position == 1;
        holder.root.setBackgroundColor(ContextCompat.getColor(this,
                warm ? R.color.brand_red_light : R.color.banner_bg));
        holder.title.setTextColor(ContextCompat.getColor(this,
                warm ? R.color.brand_red : R.color.banner_fg));
        holder.title.setText(POSTER_TITLES[catIndex]);
        holder.caption.setText(POSTER_CAPTIONS[catIndex]);
        holder.footer.setText("—— " + STORE_NAMES[catIndex] + " 出品 ——");
    }

    //===== 锚点 tab =====

    private void setupTabs() {
        //「商品」锚点就是页首；其余三个滚到各自卡片的顶边
        final View[] targets = {nsv, llReviewCard, llDescCard, llRecommendCard};
        for (int i = 0; i < tabCells.length; i++) {
            final int index = i;
            tabCells[i].setOnClickListener(v -> {
                selectAnchorTab(index);
                View target = targets[index];
                //卡片 getTop() 是相对滚动内容容器的；滚过去时目标顶边贴视口顶
                if (target == nsv) {
                    nsv.smoothScrollTo(0, 0);
                } else {
                    nsv.smoothScrollTo(0, Math.max(0, target.getTop()));
                }
            });
        }
        selectAnchorTab(0);
    }

    private void selectAnchorTab(int index) {
        for (int i = 0; i < tabCells.length; i++) {
            boolean selected = i == index;
            TextView label = (TextView) ((LinearLayout) tabCells[i]).getChildAt(0);
            label.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.brand_red : R.color.text_primary));
            label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            tabLines[i].setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    //===== 价格卡 / 标题卡 =====

    private void renderPriceCard() {
        tvPriceMain.setText(formatPrice(item.getPrice()));

        //划线价固定上浮 25%，纯演示数据；删除线在 Java 里加
        double origin = Math.round(item.getPrice() * 1.25 * 10) / 10.0;
        tvPriceOrigin.setText("原价 " + formatPrice(origin));
        tvPriceOrigin.setPaintFlags(tvPriceOrigin.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        int stock = 120 + seed() % 880;
        tvStock.setText(item.getSales() >= 10000
                ? "限量抢购中" : String.format(Locale.CHINA, "库存紧张 · 仅剩%d件", stock));
        tvPriceSales.setText("已售 " + ShopAdapter.formatSales(item.getSales()).replace("已售", "")
                + " · 好评率 99%");
    }

    private void renderTitleCard() {
        tvDetailTitle.setText(item.getTitle());
        tagViews[0].setText("运费险");
        tagViews[1].setText(ShopAdapter.formatSales(item.getSales()));
        tagViews[2].setText("好评率 99%");
    }

    //===== 服务卡 =====

    private void renderServicesCard() {
        tvServiceShip.setText("运费险 · 7天无理由退换");

        String day = seed() % 2 == 0 ? "明天" : "后天";
        String freight = item.getPrice() >= 99 ? "免运费" : "运费 6.0 元";
        tvServiceDelivery.setText("预计" + day + "送达 · " + ORIGINS[catIndex] + "发货 · " + freight);

        tvServiceSpec.setText("规格：" + specs[0] + "，共" + specs.length + "种可选");
        findViewById(R.id.ll_service_spec).setOnClickListener(v -> showSkuSheet(MODE_ADD));
    }

    //===== 评价卡 =====

    private void renderReviewsCard() {
        tvReviewHeader.setText("商品评价 (" + (item.getSales() / 10 + 300) + ")");
        tvReviewRate.setText("好评率 99% >");

        //固定两条；名字和文案的下标错开，避免两行重样
        for (int i = 0; i < 2; i++) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_detail_review, llReviewRows, false);
            int nameIdx = (seed() + i * 3) % REVIEW_NAMES.length;
            int textIdx = (seed() + i * 5) % REVIEW_TEXTS.length;

            TextView avatar = row.findViewById(R.id.tv_review_avatar);
            avatar.setText(REVIEW_NAMES[nameIdx].substring(0, 1));
            avatar.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, AVATAR_COLORS[(seed() + i) % AVATAR_COLORS.length])));

            ((TextView) row.findViewById(R.id.tv_review_name)).setText(REVIEW_NAMES[nameIdx]);
            ((TextView) row.findViewById(R.id.tv_review_text)).setText(REVIEW_TEXTS[textIdx]);
            ((TextView) row.findViewById(R.id.tv_review_date))
                    .setText(String.format(Locale.CHINA, "2026-09-%02d", 20 + (seed() + i) % 7));
            llReviewRows.addView(row);
        }
    }

    //===== 店铺卡 =====

    private void renderShopCard() {
        String storeName = STORE_NAMES[catIndex];
        tvStoreAvatar.setText(storeName.substring(0, 1));
        tvStoreAvatar.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, AVATAR_COLORS[catIndex % AVATAR_COLORS.length])));
        tvStoreName.setText(storeName);
        tvStoreScore.setText("店铺口碑 4.60 分 · 平均发货 " + (8 + seed() % 24) + " 小时");
        findViewById(R.id.btn_enter_store).setOnClickListener(v ->
                toast("演示模式：店铺主页暂未开放"));
    }

    //===== 商品详情卡 =====

    private void renderDetailSection() {
        ImageView photo = findViewById(R.id.iv_desc_photo);
        photo.setImageResource(item.getImgRes());

        String[] paragraphs = DESC_PARAGRAPHS[catIndex];
        tvDescBody1.setText(android.text.Html.fromHtml("<h3>" + "商品亮点" + "</h3><p>" + android.text.TextUtils.htmlEncode(paragraphs[0]) + "</p><p><b>品质承诺：</b><font color=\"#E53935\">校园精选，安心选购</font></p>"));
        tvDescBody2.setText(android.text.Html.fromHtml("<h3>" + "使用说明" + "</h3><p>" + android.text.TextUtils.htmlEncode(paragraphs[1]) + "</p><p><b>品质承诺：</b><font color=\"#E53935\">校园精选，安心选购</font></p>"));
        tvDescBody3.setText(android.text.Html.fromHtml("<h3>" + "服务保障" + "</h3><p>" + android.text.TextUtils.htmlEncode(paragraphs[2]) + "</p><p><b>品质承诺：</b><font color=\"#E53935\">校园精选，安心选购</font></p>"));

        //参数表：label/value 两列行 + 分隔线，行数固定所以用代码拼
        String[][] params = {
                {"商品编号", "TT" + String.format(Locale.CHINA, "%08d", 10000000 + seed() % 89999999)},
                {"商品品类", item.getCategory()},
                {"发货产地", ORIGINS[catIndex]},
                {"配送服务", "顺丰速运 · 满99包邮"},
                {"售后保障", "七天无理由退换 · 全国联保"},
        };
        int padV = getResources().getDimensionPixelSize(R.dimen.space_s);
        for (String[] pair : params) {
            TextView label = new TextView(this);
            label.setText(pair[0]);
            label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.text_body_small));
            label.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));

            TextView value = new TextView(this);
            value.setText(pair[1]);
            value.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.text_body_small));
            value.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, padV, 0, padV);
            LinearLayout.LayoutParams labelLp =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.rightMargin = getResources().getDimensionPixelSize(R.dimen.space_l);
            row.addView(label, labelLp);
            row.addView(value, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            llDescParams.addView(row);
        }
    }

    //===== 推荐流 =====

    private void setupRecommendations() {
        List<ShopItem> catalog = ShopFragment.buildCatalog();
        List<ShopItem> recommend = new ArrayList<>();
        //同品类优先（排除自己），不足 6 个再拿其他品类的补
        for (ShopItem si : catalog) {
            if (si != item && si.getCategory().equals(item.getCategory())) {
                recommend.add(si);
            }
        }
        for (ShopItem si : catalog) {
            if (recommend.size() >= 6) break;
            if (!si.getCategory().equals(item.getCategory())) {
                recommend.add(si);
            }
        }

        //和商城瀑布流同一套适配器 + 同一种 LayoutManager，观感完全一致；
        //RecyclerView 本身 wrap_content + 关嵌套滚动（布局里有注释）
        rvRecommend.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        ShopAdapter adapter = new ShopAdapter(recommend);
        adapter.setOnItemClickListener(si -> startActivity(intent(this, si)));
        rvRecommend.setAdapter(adapter);
    }

    //===== 底部操作栏 =====

    private void setupBottomBar() {
        findViewById(R.id.ll_nav_store).setOnClickListener(v ->
                toast("演示模式：店铺主页暂未开放"));

        //客服会话和订单详情页的「联系客服」走同一个入口文案
        findViewById(R.id.ll_nav_service).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_PEER_NAME, "商城官方客服");
            startActivity(intent);
        });

        findViewById(R.id.fl_nav_cart).setOnClickListener(v ->
                ShoppingDialogs.showCart(this));

        findViewById(R.id.btn_add_cart).setOnClickListener(v -> showSkuSheet(MODE_ADD));
        findViewById(R.id.btn_buy_now).setOnClickListener(v -> showSkuSheet(MODE_BUY));
    }

    private void refreshCartBadge() {
        int count = store.getCartCount();
        //空购物车不显示「0」，藏掉更干净
        tvCartBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        tvCartBadge.setText(String.valueOf(count));
    }

    //===== SKU 面板 =====

    private void showSkuSheet(int mode) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_sku);

        //窗口三件套，缺一个都会穿帮：透明背景（不透则圆角外露默认面板）、
        //宽度通栏、贴底弹出
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }
        dialog.setCanceledOnTouchOutside(true);

        ImageView thumb = dialog.findViewById(R.id.iv_sku_thumb);
        TextView price = dialog.findViewById(R.id.tv_sku_price);
        TextView spec = dialog.findViewById(R.id.tv_sku_spec);
        TextView qty = dialog.findViewById(R.id.tv_sku_qty);
        TextView confirm = dialog.findViewById(R.id.tv_sku_confirm);
        LinearLayout llSpecs = dialog.findViewById(R.id.ll_sku_specs);
        TextView minus = dialog.findViewById(R.id.btn_sku_minus);
        TextView plus = dialog.findViewById(R.id.btn_sku_plus);

        thumb.setImageResource(item.getImgRes());
        price.setText(formatPrice(item.getPrice()));

        //规格胶囊：选中态要随点击切换，只能代码建、代码换肤
        List<TextView> pills = new ArrayList<>();
        for (int i = 0; i < specs.length; i++) {
            TextView pill = new TextView(this);
            pill.setText(specs[i]);
            pill.setGravity(Gravity.CENTER);
            pill.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.text_body_small));
            int padH = getResources().getDimensionPixelSize(R.dimen.space_l);
            pill.setPadding(padH, 0, padH, 0);
            pill.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.space_2xl));
            if (i > 0) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = getResources().getDimensionPixelSize(R.dimen.space_s);
                pill.setLayoutParams(lp);
            }
            final int index = i;
            pill.setOnClickListener(v -> {
                specIndex = index;
                restyleSpecPills(pills, spec);
            });
            llSpecs.addView(pill);
            pills.add(pill);
        }
        restyleSpecPills(pills, spec);

        //数量步进：下限 1（置灰防呆），上限 99（和购物车页一致）
        Runnable renderQty = () -> {
            qty.setText(String.valueOf(skuQty));
            spec.setText("已选：" + specs[specIndex] + "，数量 " + skuQty);
            minus.setAlpha(skuQty <= 1 ? 0.4f : 1f);
        };
        minus.setOnClickListener(v -> {
            if (skuQty > 1) {
                skuQty--;
                renderQty.run();
            }
        });
        plus.setOnClickListener(v -> {
            if (skuQty < 99) {
                skuQty++;
                renderQty.run();
            }
        });
        renderQty.run();

        //确认键：加购橙 / 购买红，同一个胶囊底按模式染色
        boolean isAdd = mode == MODE_ADD;
        confirm.setText(isAdd ? "加入购物车" : "立即购买");
        confirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                isAdd ? R.color.brand_orange : R.color.brand_red)));
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (isAdd) {
                onAddToCart();
            } else {
                onBuyNow();
            }
        });

        dialog.show();
    }

    private void restyleSpecPills(List<TextView> pills, TextView specText) {
        for (int i = 0; i < pills.size(); i++) {
            boolean selected = i == specIndex;
            TextView pill = pills.get(i);
            pill.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            pill.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.brand_red : R.color.text_secondary));
            pill.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
        specText.setText("已选：" + specs[specIndex] + "，数量 " + skuQty);
    }

    private void onAddToCart() {
        store.addToCart(item.getProductId(), specs[specIndex], item.getTitle(), Math.round(item.getPrice() * 100),
                item.getImgRes(), item.getCategory(), skuQty);
        toast("已加入购物车");
        refreshCartBadge(); //不跳页就没有 onResume，得手动刷
    }

    private void onBuyNow() {
        Intent intent = new Intent(this, OrderConfirmActivity.class);
        intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_ID, item.getProductId());
        intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_SKU, specs[specIndex]);
        intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_TITLE, item.getTitle());
        intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_PRICE_CENTS, Math.round(item.getPrice() * 100));
        intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_IMG, item.getImgRes());
        intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_CAT, item.getCategory());
        intent.putExtra(OrderConfirmActivity.EXTRA_DIRECT_QTY, skuQty);
        //不 finish：付完返回还能停在这件商品上，符合电商页习惯
        startActivity(intent);
    }

    //===== 小工具 =====

    /** 标题 hash 取正（& 0x7fffffff 而不是 Math.abs，躲开 MIN_VALUE 取反溢出），
     *  用来做「同一商品每次进来展示内容一致」的确定性伪随机 */
    private int seed() {
        return item.getTitle().hashCode() & 0x7fffffff;
    }

    private int catIndexOf(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                return i;
            }
        }
        return 0;
    }

    private static String formatPrice(double price) {
        return String.format(Locale.CHINA, "¥%.2f", price);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
