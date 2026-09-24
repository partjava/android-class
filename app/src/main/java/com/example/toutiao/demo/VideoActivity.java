package com.example.toutiao.demo;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频页，底部导航「视频」的落地页。
 *
 * 四个频道（推荐 / 小视频 / 影视 / 音乐）各占 ViewPager2 的一页，
 * 左右滑动换频道，点标签也能换，两者双向同步。
 *
 * 为什么用 ViewPager2 而不是照抄商城页的「RecyclerView + PagerSnapHelper」：
 * 两条路都能做「一页一屏」（PagerSnapHelper 在本项目已有先例，见
 * ShopActivity），这里多要的一样东西是 onPageScrolled 的**连续偏移量**——
 * 下一步顶栏淡出、遮罩渐入都要靠它按手指位置逐帧驱动。
 * 手写的话得自己量 computeHorizontalScrollOffset 再反推，还得自己处理
 * 「点标签跳页」时的滚动驱动和页码语义。androidx.viewpager2 由
 * material:1.5.0 传递引入，本来就在 classpath 上。
 *
 * 4 页里**只有推荐页是另一种形态**：竖向全屏流，一屏一个视频、上下滑切换。
 * 另外三个频道保持双列网格。所以 VideoChannelAdapter 里有两条 viewType
 * 分支，两条路用的是两个不同的 adapter（VideoFeedAdapter / VideoAdapter）。
 *
 * · 频道标签：复用了商城页的 item_channel_tab 布局，选中带红下划线
 * · 数据：4 个频道各持有一份完整列表，**每页一个独立的 VideoAdapter**
 *   （理由见 VideoChannelAdapter 的类注释，这条是必须的，不是风格）
 * · 网格页点击某条视频 → VideoDetailActivity 播放页
 * · 推荐页单击整屏就地播放/暂停（模拟进度条），**不跳页**；上下滑换条时
 *   新的从 0 开始、旧的复位。播放状态全在 VideoFeedAdapter 里
 *
 * 这一页是全项目唯一一个「内容铺进状态栏底下」的浅色页面：
 * setDecorFitsSystemWindows(false) + 透明状态栏 + 白色图标，
 * 红顶栏自己补状态栏高度的内边距把红底铺上去。滑到推荐页时顶栏淡出、
 * 那一块露出来的就是封面，同时频道标签换成「压在封面上」的那套配色。
 * 见 setupImmersive() / applyWindowInsets() / applyChrome()。
 */
public class VideoActivity extends AppCompatActivity {

    //频道标签，每个频道在 channelData 里对应一份同样下标的列表
    private static final String[] TAB_LABELS = {"推荐", "小视频", "影视", "音乐"};
    private static final int DEFAULT_TAB = 0;

    private ViewPager2 vpChannels;
    private LinearLayout llChrome;
    private LinearLayout topBar;
    private View vFeedScrim;
    private HorizontalScrollView hsTabs;
    private LinearLayout llTabs;
    private BottomNavigationView bottomNav;

    private final List<View> tabViews = new ArrayList<>();
    //每个频道各自的数据，下标和 TAB_LABELS 对齐
    private final List<List<VideoItem>> channelData = new ArrayList<>();

    private VideoChannelAdapter channelAdapter;
    //推荐页那一屏的全屏流。由 Activity 持有（不是每页 new 一个），
    //这样 ViewPager2 销毁离屏页重建 RecyclerView 时，播放进度还活着
    private VideoFeedAdapter feedAdapter;

    //当前高亮的标签下标。applyChrome() 翻转配色时要靠它把选中态重画一遍
    private int currentTab = DEFAULT_TAB;

    //chrome 现在是不是「压在封面上」那一套。只在这一个布尔值翻转时才重画标签，
    //不是每帧都画——理由见 applyChrome()
    private boolean overFeed;

    //标签 item 的按压波纹。深色那套是项目自己的 drawable，
    //浅色那套就是 item_channel_tab.xml 里写的 ?attr/selectableItemBackground，
    //这里把它的**资源 id**解析出来缓存住（不是缓存 Drawable 实例，见 applyTabStyle）
    private int rippleLightRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //沉浸式必须在 setContentView 之前做，理由同 VideoDetailActivity
        setupImmersive();
        setContentView(R.layout.activity_video);
        bindView();
        initVideoData();
        setupTabs();
        bindEvent();
        applyWindowInsets();
        measureChrome();
        //默认页就是推荐页，所以启动时 chrome 应当已经是「压在封面上」那一套。
        //不等 onPageScrolled 回调：它虽然会来（实测启动时会发一次
        //onPageScrolled(0, 0.0, 0)），但那是 ViewPager2 的实现细节，
        //依赖它会让首帧的顶栏/标签配色变成「碰巧对」。这里显式定死。
        applyChrome(1f);
    }

    /**
     * 页面被别的页盖住（点底部导航去了商城等）→ 停表、留着进度。
     * 回来时 onResume 接着播。
     *
     * ⚠️ 这里不能用 setActive(false)：那会把 active 也置成 false，
     *    onResume 就不会重新开播了。走的是「按了暂停」那条路。
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (feedAdapter != null) feedAdapter.onHostPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (feedAdapter != null) feedAdapter.onHostResume();
        //⚠️ 每次回来都要拨一次，不能只在 onCreate 里设一次（见 syncNavSelection）
        syncNavSelection();
    }

    /**
     * 把底部导航的高亮拨回「视频」。
     *
     * ⚠️ 这件事必须在 onResume 里做，只在 onCreate 里设一次是不够的：
     *    点「商城」离开本页时，被选中的是「商城」那一项
     *    （BottomNavigationView 在回调之前就把自己选好了），而本实例
     *    **不会**被销毁——不管是按返回键回来，还是在商城页点「视频」
     *    （CLEAR_TOP|SINGLE_TOP）复用本实例回来，onCreate 都不跑，
     *    高亮就一直留在「商城」上。实测两条路都必现。
     *
     * ⚠️ 用 MenuItem.setChecked(true)，**不要**用 setSelectedItemId()：
     *    后者会把「点击」重放一遍，触发 OnItemSelectedListener，而那个
     *    监听器的 nav_video 分支是弹「当前在视频页」——一进页面就冒一个
     *    Toast。setChecked 只改选中态、不派发动作，同一个 group 里的
     *    其它项由 MenuBuilder 自动取消选中。
     */
    private void syncNavSelection() {
        bottomNav.getMenu().findItem(R.id.nav_video).setChecked(true);
    }

    /**
     * ⚠️ release() 不能省。全屏流的 Handler 挂在主线程 Looper 上，
     *    不 removeCallbacksAndMessages(null) 的话，被销毁的 adapter
     *    还会每 200ms 醒一次，并且去碰一个已经没了的 RecyclerView。
     *
     *    走底部导航去首页时这个页面会被 CLEAR_TOP 清掉，走的就是这条路。
     */
    @Override
    protected void onDestroy() {
        if (feedAdapter != null) feedAdapter.release();
        super.onDestroy();
    }

    private void bindView() {
        vpChannels = findViewById(R.id.vp_channels);
        llChrome = findViewById(R.id.ll_chrome);
        topBar = findViewById(R.id.top_bar);
        vFeedScrim = findViewById(R.id.v_feed_scrim);
        hsTabs = findViewById(R.id.hs_video_tabs);
        llTabs = findViewById(R.id.ll_video_tabs);
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void initVideoData() {
        channelData.add(buildRecommend());
        channelData.add(buildSmall());
        channelData.add(buildMovie());
        channelData.add(buildMusic());

        //点击某条视频进入播放页，带过去 6 个 extra。
        //这一份回调传给 VideoChannelAdapter，由它转给每一页自己的 VideoAdapter
        VideoAdapter.OnItemClickListener openDetail = item -> {
            Intent intent = new Intent(VideoActivity.this, VideoDetailActivity.class);
            intent.putExtra(VideoDetailActivity.EXTRA_TITLE, item.getTitle());
            intent.putExtra(VideoDetailActivity.EXTRA_SOURCE, item.getSource());
            intent.putExtra(VideoDetailActivity.EXTRA_DURATION, item.getDuration());
            intent.putExtra(VideoDetailActivity.EXTRA_PLAY_COUNT, item.getPlayCount());
            intent.putExtra(VideoDetailActivity.EXTRA_DESC, item.getDesc());
            intent.putExtra(VideoDetailActivity.EXTRA_COVER, item.getCoverRes());
            startActivity(intent);
        };

        //推荐页（第 0 页）是全屏流，用自己那一份数据。这个实例是单例，
        //传进 VideoChannelAdapter 由它挂到第 0 页那个 RecyclerView 上
        feedAdapter = new VideoFeedAdapter(channelData.get(DEFAULT_TAB));
        channelAdapter = new VideoChannelAdapter(channelData, openDetail, feedAdapter);
        vpChannels.setAdapter(channelAdapter);

        //竖滑和横滑会抢手势：推荐页整页是竖向 RecyclerView，手指稍微斜一点
        //就可能在「想上下滑视频」时被外层翻页接走。理想解法是
        //vpChannels.setScrollingTouchSlop(TOUCH_SLOP_PAGING)，但那个方法
        //是 viewpager2 **1.1.0** 才有的，本项目走 material:1.5.0 传递进来的
        //是 1.0.0（javap 核过：连 TOUCH_SLOP_PAGING 常量都没有）。
        //为一行调参去动 build.gradle 不划算，先靠 ViewPager2 自己的
        //方向判定（横向位移超过 touchSlop 才接管），实测够用。
        //真出现抢手势再回来加显式依赖。

        vpChannels.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            /**
             * 翻页 → 高亮标签 + 滚到可见区域。
             *
             * ⚠️ 这里只做这两件事，别的状态一个都不要加。ViewPager2 的
             *    notifyProgrammaticScroll 会把 onPageSelected **提前**发出来
             *    （早于动画），如果在 onPageSelected 和 onPageScrolled 里
             *    各管一套状态，点标签跳页时会先闪一下。
             *    高亮是幂等的，提前发无所谓；但"顶栏淡到几成"这类跟手指走的
             *    东西必须放在 onPageScrolled 里按 t 驱动。
             */
            @Override
            public void onPageSelected(int position) {
                highlightTab(position);
                scrollTabIntoView(position);
            }

            /**
             * 跟手指走的连续进度，chrome 全靠它驱动。
             *
             * t = 1 表示「完全在推荐页」。position 是**当前占主导的那一页**：
             * 停在推荐页时是 (0, 0.0)，正往小视频滑到一半是 (0, 0.5)，
             * 已经停在小视频是 (1, 0.0)。所以只有 position==0 时 t 才有值，
             * 其余一律取 0——从第 2 页往回滑时 position 会重新变成 0、
             * offset 从 1 递减到 0，t 正好从 0 涨回 1，方向是对称的。
             */
            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
                float t = (position == DEFAULT_TAB) ? 1f - offset : 0f;
                //浮点误差下 offset 可能到 1.0000001，不夹住的话 alpha 会 <0
                applyChrome(Math.max(0f, Math.min(1f, t)));
            }
        });
    }

    //===== 各频道的数据 =====
    /**
     * 封面的选图说明（重要）：
     *
     * 项目里**没有一张横构图的高清图**——image* 是分辨率最高的一组，
     * 但全是竖构图或近方图。封面框是 16:9（见 item_video.xml，
     * 双列一格 160dp 宽 → 封面 160 × 9 ÷ 16 = 90dp），
     * centerCrop 之后保留的原图高度 = 宽高比 ÷ 1.778：
     *
     *     image7  939×925   保留 57%   ← 最理想，又清晰又裁得少
     *     image2  1080×1447 保留 42%   ← 分辨率最高
     *     image5  960×1280  保留 42%
     *     image8  960×1358  保留 40%
     *     img4   784×944    保留 47%
     *     image6  1080×1621 保留 37%
     *
     * 所以只用这 6 张。刻意**不用** image1 / image4（640×1386、1000×2166），
     * 它们裁到 16:9 只剩 26% 的高度，基本就是一条横带。
     *
     * 另外标题都写成「纪录片 / 影像 / 现场」这类**与具体画面无关**的措辞，
     * 这样无论裁出中间哪一块，标题都说得通，不会出现图文错位。
     *
     * 改成双列之后封面只剩 90dp 高，比改造前的 184dp 矮了一半——
     * 但**裁切的影响反而更小了**：一行「剩下多少百分比」在 184dp 的大图上
     * 一眼能看出被削平，在 90dp 的小图上只是个缩略图，没人去比对原图。
     * 所以这份「只用这 6 张」的白名单没变，还是不用 image1 / image4。
     *
     * 每个频道内封面不重复；跨频道复用。
     *
     * 每条末尾那两个数字是**点赞数、评论数**（VideoItem 的最后两个参数，
     * 顺序别写反，两个都是 int，写反了编译器不报错）。几点约定：
     *
     * · 点赞数一律取在几千这个量级，不上万。上万之后操作栏显示的是
     *   「1.3万」，用户点一下加一，文本不会变，看着像没反应——
     *   这是「按万取整」的代价，README 的已知遗留里记了一笔。
     * · 点赞数远小于同一行的播放量（0.3% ~ 1% 这个量级），
     *   108 万播放配 8600 赞，比「播放 100 万、点赞 80 万」可信得多。
     * · 评论数通常只有点赞数的零头，采访类、教程类是例外
     *   （「街头采访」「家常菜」那两条评论写得比点赞多，是有意的）。
     */

    private List<VideoItem> buildRecommend() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("纪录片：大国重器是怎样炼成的", "央视新闻", "12:36", "128万次播放",
                "镜头走进重型装备制造车间，记录一台大型设备从零件加工到整机装配的全过程。工人师傅们在微米级的精度要求下，一遍遍测量、打磨。",
                R.drawable.image2, 8600, 326));
        list.add(new VideoItem("延时摄影记录城市从黎明到黄昏", "影像志", "04:12", "56万次播放",
                "摄影师在城市最高处架设机位，连续拍摄十二个小时。整座城市的一天被压缩进短短几分钟。",
                R.drawable.image7, 3200, 168));
        list.add(new VideoItem("航拍中国：从空中俯瞰大好河山", "航拍视角", "08:45", "92万次播放",
                "镜头掠过奔腾的江河、金黄的麦田和错落的村落，山川河流呈现出与地面完全不同的壮阔面貌。",
                R.drawable.image5, 5400, 271));
        list.add(new VideoItem("实拍：消防员火场逆行救人全过程", "现场实录", "06:03", "213万次播放",
                "浓烟从窗口不断涌出。消防员沿着楼道逐层搜救，在五楼卧室内找到一名被困老人，背起老人沿原路撤离，整个过程不到六分钟。",
                R.drawable.img4, 9600, 412));
        list.add(new VideoItem("街头采访：你理想中的生活是什么样的", "城市观察", "09:28", "34万次播放",
                "记者在街头随机采访了二十位路人。答案各不相同，但大多朴素而具体。",
                R.drawable.image8, 2100, 486));
        return list;
    }

    private List<VideoItem> buildSmall() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("萌宠日常：猫咪第一次见到雪的反应", "萌宠日记", "00:15", "320万次播放",
                "第一次见到雪的猫咪先是愣在原地，随后小心翼翼地伸出一只爪子试探，最后还是忍不住扑了进去。",
                R.drawable.image5, 9800, 356));
        list.add(new VideoItem("一分钟学会三道家常菜，新手零失败", "厨房小窍门", "00:58", "88万次播放",
                "西红柿炒蛋、蒜蓉青菜、可乐鸡翅，三步一步不落，照着做就行。",
                R.drawable.img4, 4200, 512));
        list.add(new VideoItem("手艺人用木头做出会走的小机器人", "手工达人", "00:42", "156万次播放",
                "全程不用一颗钉子、一滴胶水，所有零件靠榫卯咬合。上紧发条后，机器人能稳稳走上好几米。",
                R.drawable.image7, 7300, 289));
        list.add(new VideoItem("街头钢琴：路人即兴弹奏惊艳全场", "街头艺术", "00:33", "271万次播放",
                "琴键摆在广场中央，谁都可以坐下来弹一段。这位路人坐下后，围观的人群渐渐安静了。",
                R.drawable.image8, 8900, 401));
        list.add(new VideoItem("慢镜头：雨天窗外的十分钟", "慢镜头", "00:21", "19万次播放",
                "雨滴砸在玻璃上，慢慢汇成一道道水痕。没有旁白，只有雨声。",
                R.drawable.image2, 1200, 87));
        return list;
    }

    private List<VideoItem> buildMovie() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("话剧《茶馆》复排上演 幕后纪录", "戏剧观察", "15:20", "45万次播放",
                "本轮复排保留了经典版本的结构，同时在舞台呈现上做了新的尝试。镜头跟拍排练厅里的两个月。",
                R.drawable.image6, 2600, 143));
        list.add(new VideoItem("电影幕后：一场雨戏是怎么拍出来的", "影视工业", "11:08", "67万次播放",
                "洒水车、灯光组、摄影机轨道，一场看起来只有两分钟的雨戏，背后是四十多人的配合。",
                R.drawable.image2, 3400, 197));
        list.add(new VideoItem("老片修复：让经典重新清晰起来", "电影资料馆", "18:33", "28万次播放",
                "修复师逐帧处理划痕和噪点，一部两小时的老电影往往需要几个月才能完成。",
                R.drawable.image8, 1800, 96));
        list.add(new VideoItem("配音演员现场：一个人配出整部剧", "声音工坊", "07:56", "103万次播放",
                "同一个人的声音在不同角色之间来回切换，录音棚外的观众听得目瞪口呆。",
                R.drawable.img4, 6700, 318));
        list.add(new VideoItem("经典配乐赏析：旋律如何讲故事", "电影音乐", "13:14", "39万次播放",
                "从主题动机到配器选择，拆解几段耳熟能详的旋律是怎么把情绪推上去的。",
                R.drawable.image5, 2300, 152));
        return list;
    }

    private List<VideoItem> buildMusic() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("国风音乐盛典：传统乐器演绎流行金曲", "音乐现场", "22:41", "74万次播放",
                "古筝、琵琶、笛箫与现代编曲结合，重新演绎了多首流行歌曲。熟悉的旋律换一种方式演奏，别有一番味道。",
                R.drawable.image7, 4800, 236));
        list.add(new VideoItem("民谣现场：一把吉他唱完整个夏天", "livehouse", "05:37", "51万次播放",
                "小小的场地里挤满了人，唱到副歌时全场跟着一起哼。",
                R.drawable.image5, 2900, 174));
        list.add(new VideoItem("交响乐团排练直击：指挥到底在说什么", "古典频道", "09:02", "22万次播放",
                "排练时指挥不断叫停，一遍遍调整某个声部的力度和进入时机。",
                R.drawable.image6, 1500, 118));
        list.add(new VideoItem("草原上的长调：一个人的合唱", "民歌采集", "06:48", "18万次播放",
                "老人在空旷的草原上唱起长调，声音传得很远。录音师说，这种唱法已经很少有人会了。",
                R.drawable.image2, 940, 63));
        list.add(new VideoItem("深夜电台：城市入睡之后的声音", "声音志", "28:15", "63万次播放",
                "凌晨两点的便利店、收摊的夜市、最后一班公交，把这些声音拼在一起，就是一座城市的深夜。",
                R.drawable.image8, 3600, 208));
        return list;
    }

    //==== 频道标签：和商城页同一套交互 ====

    private void setupTabs() {
        cacheTabRipples();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < TAB_LABELS.length; i++) {
            View tab = inflater.inflate(R.layout.item_channel_tab, llTabs, false);
            ((TextView) tab.findViewById(R.id.tv_tab_label)).setText(TAB_LABELS[i]);
            final int index = i;
            tab.setOnClickListener(v -> selectTab(index));
            llTabs.addView(tab);
            tabViews.add(tab);
        }
        //首屏的高亮得手动点一次：启动时没有翻页动作，onPageSelected 不会触发
        highlightTab(DEFAULT_TAB);
    }

    /**
     * 解析出 item_channel_tab.xml 里那个 ?attr/selectableItemBackground
     * 对应的资源 id，缓存住。
     *
     * ⚠️ 缓存的是**资源 id**，不是 Drawable 实例。
     *    一个 Drawable 实例只能挂在一个 View 上——View.setBackground() 会把
     *    它的 callback 指向自己、并占用它的 bounds，4 个标签共用同一个实例的话
     *    波纹会画到错误的位置（或者干脆不画）。所以每次重画标签都重新
     *    getDrawable() 一次，拿 4 个互相独立的实例。
     *
     * ⚠️ 不用 getResources().getDrawable(...)：那是 API 21 起废弃的写法。
     *    TypedValue 是解析主题属性（?attr/...）的标准姿势，必须带第三个参数
     *    resolveRefs=true，否则拿回来的是引用本身而不是它指向的资源。
     */
    private void cacheTabRipples() {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        //主题没定义这个属性时 resourceId 是 0，退回「没有按压反馈」而不是崩
        rippleLightRes = tv.resourceId;
    }

    /**
     * 点标签 = 请 ViewPager2 翻到那一页，**不再自己换数据**。
     *
     * 高亮不在这里做：setCurrentItem 会触发 onPageSelected，由那一处统一
     * 调 highlightTab()。这样无论「点标签」还是「手动滑」，高亮都只有
     * 一个入口，不会出现两条路径各改一遍、状态对不上的情况。
     *
     * 点当前已经在的那一页时 ViewPager2 不发回调——但那也不需要高亮，
     * 本来就是选中态。
     */
    private void selectTab(int index) {
        vpChannels.setCurrentItem(index, true);
    }

    /**
     * 只画标签的选中态，不碰数据、不碰页码、也**不滚动**标签条。
     *
     * ⚠️ 配色有两套，取决于 chrome 现在压在哪上面（overFeed）：
     *    压在白底标签条上是「选中红、未选深灰」；
     *    压在封面/深色遮罩上是「选中白、未选 #CCCCCC」——
     *    红色压深色遮罩只有约 2.5:1，白字才有 8:1。
     *    所以这个方法不能缓存结果，换配色之后必须整体重画一次
     *    （applyTabStyle 就是这么用的）。
     *
     * 滚动单独放在 scrollTabIntoView()：这一处会在一次横滑里被调用两次
     * （配色翻转），如果顺带触发 smoothScrollTo，会一边滑页一边滚标签条。
     */
    private void highlightTab(int index) {
        currentTab = index;
        int selectedColor = overFeed ? R.color.text_on_dark : R.color.brand_red;
        int normalColor = overFeed ? R.color.text_on_dark_secondary : R.color.text_primary;
        for (int i = 0; i < tabViews.size(); i++) {
            boolean selected = i == index;
            TextView tv = tabViews.get(i).findViewById(R.id.tv_tab_label);
            View line = tabViews.get(i).findViewById(R.id.v_tab_line);
            tv.setTextColor(ContextCompat.getColor(this, selected ? selectedColor : normalColor));
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            line.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    //把选中的标签滚到可见区域中间。post() 是等测量完成后宽度才有值
    private void scrollTabIntoView(int index) {
        View tab = tabViews.get(index);
        hsTabs.post(() -> hsTabs.smoothScrollTo(
                Math.max(0, (tab.getLeft() + tab.getRight()) / 2 - hsTabs.getWidth() / 2), 0));
    }

    //==== 横滑时 chrome 的变化 ====

    /**
     * chrome 的唯一驱动入口。t=1 表示「完全在推荐页」，0 表示「完全在网格页」。
     *
     * 三样东西用两种方式跟着走，这个区别是有意的：
     *
     * · 顶栏和遮罩**连续**跟手指 —— 它们本来就是渐变 / 位移。
     *   遮罩要 2 倍速：翻转点 t=0.5 时正好吃满 70% 黑，白字才有 8:1；
     *   不加速的话 t=0.5 只有 35% 黑，白字压上去是 3.9:1，不够看。
     *
     * · 标签**离散翻转**，不能交叉渐变 —— 半透明白底和半透明白字各自
     *   线性插值时，中间帧会出现「半透明白字压在半透明白底」，
     *   实测对比度约 2:1。那不是观感问题，是「横滑到一半标签看不见」。
     *   离散翻转还顺带省掉了逐帧 valueOf 的 ColorStateList 分配
     *   （一次横滑 4 个标签 × 60 帧 ≈ 240 个小对象）。
     */
    private void applyChrome(float t) {
        //⚠️ 顺序：先 setAlpha 再切 VISIBLE。反过来的话，从网格页滑回来那一帧
        //   顶栏会先以 alpha=0 变可见、下一帧才被设成正确值。
        topBar.setAlpha(1f - t);

        //⚠️ alpha=0 的 View **仍然吃点击**。顶栏里那个假搜索框如果不切
        //   INVISIBLE，在推荐页点那个位置会莫名弹「搜索功能仅为演示」。
        //   两个方向都要管——只设 INVISIBLE 不设回来，滑回网格页搜索框就死了。
        //   用 INVISIBLE 不用 GONE：高度得留着，网格页的 paddingTop 是量出来的。
        topBar.setVisibility(t >= 0.5f ? View.INVISIBLE : View.VISIBLE);

        vFeedScrim.setAlpha(Math.min(1f, t * 2f));

        boolean wantOverFeed = t >= 0.5f;
        if (wantOverFeed != overFeed) {
            overFeed = wantOverFeed;
            applyTabStyle();
            //全屏流的播放状态和标签配色挂在**同一个**翻转点上，不另外找时机。
            //理由见 VideoFeedAdapter.setActive()：onPageSelected 会被 ViewPager2
            //提前发出来，按它开播/停播会在页面还没滑走时就把进度归零。
            if (feedAdapter != null) feedAdapter.setActive(overFeed);
        }
    }

    /**
     * 切换标签条那一整套配色（底色 / 文字色 / 字重 / 下划线 / 按压波纹）。
     *
     * ⚠️ 只在离散翻转时调用，不是每帧。setTextColor 只 invalidate 不
     *    requestLayout，但 4 个标签 × 2 个 View 的 findViewById 每帧来一遍
     *    是白扔的。
     */
    private void applyTabStyle() {
        //压封面时整条标签条透明，露出来的就是封面 + 顶部遮罩；
        //网格页时恢复白底，和商城页的标签条一致
        hsTabs.setBackgroundColor(overFeed ? Color.TRANSPARENT
                : ContextCompat.getColor(this, R.color.bg_surface));

        for (View tab : tabViews) {
            //⚠️ 按压波纹也得跟着换。?attr/selectableItemBackground 的波纹色是
            //   Light 主题的 colorControlHighlight（12% 黑），压在封面和深色遮罩上
            //   完全看不见，点上去像没反应——和播放页「相关视频」在黑底上失效
            //   是同一类问题，解药也一样（bg_ripple_on_dark，12% 白）。
            //   每个标签都要一张**独立**的 Drawable，理由见 cacheTabRipples()。
            tab.setBackground(ripple(overFeed ? R.drawable.bg_ripple_on_dark : rippleLightRes));
        }

        //文字色分两套，换完得把选中态整体重画一遍
        highlightTab(currentTab);
    }

    //资源 id 为 0 说明主题里没定义（见 cacheTabRipples），此时清掉背景即可：
    //没有按压反馈，但不会崩
    private Drawable ripple(int resId) {
        return resId == 0 ? null : ContextCompat.getDrawable(this, resId);
    }

    //==== 沉浸式：内容铺进状态栏底下，但不隐藏状态栏 ====

    /**
     * 和 VideoDetailActivity.setupImmersive() 同一套 androidx.core compat 写法
     * （平台的 WindowInsetsController 是 API 30+，minSdk 是 29，直接用会崩）。
     *
     * 区别只有两点：
     * 1. **不 hide 状态栏**。播放页要的是整屏黑，这里要的是封面铺到顶、
     *    状态栏图标浮在封面上，状态栏本身必须还在。
     * 2. 窗口底色保持主题的白。这一页不是黑底页，露出白底不刺眼；
     *    网格页整页都有背景色，推荐页由 item 自己铺满。
     *
     * ⚠️ 白色状态栏图标：主题是 Light 主题，默认给的是深色图标。
     *    网格页压在红顶栏上是够看的，但推荐页顶栏淡出后压在**任意一张封面**上，
     *    深色图标随时可能糊掉。统一用白图标——红底上白图标对比度也够。
     *
     * ⚠️ 不能用 windowTranslucentStatus。那会把状态栏变成半透明黑，
     *    还会强制 fitSystemWindows，和本方案互斥。
     */
    private void setupImmersive() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            //返回值是 @Nullable，判空不能省（API 30+ 那条分支系统可能给不出）
            controller.setAppearanceLightStatusBars(false);
        }
    }

    /**
     * 给红顶栏补上状态栏高度的内边距，让它的红底铺进状态栏区域。
     *
     * ⚠️ 必须用监听器，不能在 onCreate 里读一次 getRootWindowInsets()：
     *    那一刻视图还没 attach、第一次 traversal 也还没发生，读回来是 null。
     *
     * ⚠️ statusBars 要带上 displayCutout：状态栏没隐藏，但刘海机器上
     *    挖孔本身就是额外遮挡。
     *
     * ⚠️ 用绝对值 setPadding，**不要**写 += ——系统栏状态变化时这个回调
     *    会反复触发，累加的话顶栏会一次比一次高。这是这套 API 最经典的坑。
     *
     *    这条原来**真的写错了**：上面那行是 topBar.getPaddingTop() + bars.top，
     *    读-改-写，正是这里警告的写法。开一次刘海模拟再关掉，顶栏就高了
     *    一个状态栏，再开一次再高一个。现在的写法是把样式里那个基准内边距
     *    （Widget.Toutiao.TopBar 的 paddingVertical，8dp）在装监听器之前
     *    取一次存成 final，回调里拿「基准值 + bars.top」绝对赋值。
     *    左右下三边本来就是读当前值再原样写回，没有累加问题。
     *
     * ⚠️ 基准值必须在**装监听器之前**取：监听器一旦挂上，系统随时可能
     *    派发一次 insets（这一页是 setDecorFitsSystemWindows(false) 的，
     *    第一次遍历就会来），那时候读到的 paddingTop 已经被加过一遍了。
     *
     * ⚠️ return insets 不能省，**不消费**。BottomNavigationView 组件自己
     *    装的 insets 监听器要靠这一份 bottom inset 把内容抬到系统导航条之上，
     *    在这里消费掉的话底部导航会被导航条盖住。
     */
    private void applyWindowInsets() {
        final View root = findViewById(R.id.root_video);
        //样式里的基准内边距，只在这里读一次（见上面第二条 ⚠️）
        final int basePaddingTop = topBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            topBar.setPadding(topBar.getPaddingLeft(),
                    basePaddingTop + bars.top,
                    topBar.getPaddingRight(),
                    topBar.getPaddingBottom());
            return insets;
        });
    }

    /**
     * 量出 chrome（顶栏 + 标签）和底部导航的实际高度，交给网格页当上下内边距。
     *
     * 改造前这两个高度是 activity_video.xml 里靠 ConstraintLayout 约束出来的，
     * 现在内容整页铺满，只能反过来算——而且**必须量**：顶栏高度取决于
     * 状态栏 inset，底部导航高度取决于系统导航条 inset，
     * 两个都不是编译期常量（不同机器、横竖屏都不一样）。
     *
     * ⚠️ 是「每遍 layout 都重量一次」，不是「post 一次量一次」。
     *
     *    原因：底部导航自己的高度也是在**第一遍 layout 之后**才被 inset 撑起来的。
     *    它内部装着 insets 监听器，会把系统导航条那 63px 加到自己的 paddingBottom
     *    上，高度从 147px 变成 210px。而 post() 只保证「在第一遍 layout 之后」，
     *    不保证「在那次带 inset 的重排之后」——实测两者会错开一帧，量回来的
     *    147px 比真实的 210px 少 63px。结果是推荐页的元信息行和那条 3dp 进度条
     *    压在底部导航底下（深色模式切换、Activity 重建后必现，因为那条路径上
     *    第一遍 layout 和 inset 派发的先后会变）。
     *
     *    每遍都量就不会错：量到的值和上次一样时，下游两个 setter 都在第一行
     *    直接返回（见 setGridPadding / setItemPaddingBottom 的幂等注释），
     *    不触发重排，所以不会来回震荡。
     *
     *    ViewTreeObserver 是整个窗口共享的，挂在内容根节点上和挂在 decor 上等价。
     */
    private void measureChrome() {
        final View root = findViewById(R.id.root_video);
        root.getViewTreeObserver().addOnGlobalLayoutListener(this::applyChromePadding);
    }

    //量一次并下发。只由 measureChrome() 挂上的 layout 监听器调用
    private void applyChromePadding() {
        int padHorizontal = getResources().getDimensionPixelSize(R.dimen.space_m);
        channelAdapter.setGridPadding(llChrome.getHeight(), bottomNav.getHeight(), padHorizontal);
    }

    //==== 搜索入口 + 底部导航 ====

    private void bindEvent() {
        //顶栏搜索入口只是个占位，不做真正的搜索
        TextView tvSearchHint = findViewById(R.id.tv_search_hint);
        tvSearchHint.setOnClickListener(v ->
                Toast.makeText(VideoActivity.this, "搜索功能仅为演示", Toast.LENGTH_SHORT).show());

        //==== 底部导航：写法和首页/商城/我的页保持一致 ====
        //启动时先把高亮放在「视频」上；之后每次回本页由 onResume 再拨一次
        syncNavSelection();
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    //复用栈里已有的首页；CLEAR_TOP 会把视频页自己清出返回栈
                    Intent intent = new Intent(VideoActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else if (itemId == R.id.nav_video) {
                    Toast.makeText(VideoActivity.this, "当前在视频页", Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.nav_add) {
                    Toast.makeText(VideoActivity.this, "点击发布", Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.nav_shop) {
                    Intent intent = new Intent(VideoActivity.this, ShopActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else if (itemId == R.id.nav_mine) {
                    Intent intent = new Intent(VideoActivity.this, MineActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                return true;
            }
        });
    }
}
