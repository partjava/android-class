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
import androidx.fragment.app.Fragment;
import android.view.ViewGroup;
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

public class VideoFragment extends PageFragment {
    @Override public View onCreateView(android.view.LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_video, parent, false);
    }

    private static final String[] TAB_LABELS = {"推荐", "小视频", "影视", "音乐"};
    private static final int DEFAULT_TAB = 0;

    private ViewPager2 vpChannels;
    private LinearLayout llChrome;
    private LinearLayout topBar;
    private View vFeedScrim;
    private HorizontalScrollView hsTabs;
    private LinearLayout llTabs;

    private final List<View> tabViews = new ArrayList<>();

    private final List<List<VideoItem>> channelData = new ArrayList<>();

    private VideoChannelAdapter channelAdapter;

    private VideoFeedAdapter feedAdapter;

    private int currentTab = DEFAULT_TAB;

    private boolean overFeed;

    private int rippleLightRes;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabViews.clear();
        channelData.clear();
        overFeed = false;
        bindView();
        initVideoData();
        setupTabs();
        bindEvent();

        measureChrome();

        int restored = savedInstanceState == null ? currentTab : savedInstanceState.getInt("channel", 0);
        vpChannels.setCurrentItem(restored, false);
        applyChrome(restored == 0 ? 1f : 0f);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (feedAdapter != null) feedAdapter.onHostPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (feedAdapter != null) feedAdapter.onHostResume();

    }

    @Override
    public void onDestroyView() {
        if (feedAdapter != null) feedAdapter.release();
        super.onDestroyView();
    }

    private void bindView() {
        vpChannels = findViewById(R.id.vp_channels);
        llChrome = findViewById(R.id.ll_chrome);
        topBar = findViewById(R.id.top_bar);
        vFeedScrim = findViewById(R.id.v_feed_scrim);
        hsTabs = findViewById(R.id.hs_video_tabs);
        llTabs = findViewById(R.id.ll_video_tabs);
    }

    private void initVideoData() {
        channelData.add(buildRecommend());
        channelData.add(buildSmall());
        channelData.add(buildMovie());
        channelData.add(buildMusic());

        VideoAdapter.OnItemClickListener openDetail = item -> {
            Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
            intent.putExtra(VideoDetailActivity.EXTRA_TITLE, item.getTitle());
            intent.putExtra(VideoDetailActivity.EXTRA_SOURCE, item.getSource());
            intent.putExtra(VideoDetailActivity.EXTRA_DURATION, item.getDuration());
            intent.putExtra(VideoDetailActivity.EXTRA_PLAY_COUNT, item.getPlayCount());
            intent.putExtra(VideoDetailActivity.EXTRA_DESC, item.getDesc());
            intent.putExtra(VideoDetailActivity.EXTRA_COVER, item.getCoverRes());
            startActivity(intent);
        };

        feedAdapter = new VideoFeedAdapter(channelData.get(DEFAULT_TAB));
        channelAdapter = new VideoChannelAdapter(channelData, openDetail, feedAdapter);
        vpChannels.setAdapter(channelAdapter);

        vpChannels.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            
            @Override
            public void onPageSelected(int position) {
                highlightTab(position);
                scrollTabIntoView(position);
            }

            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
                float t = (position == DEFAULT_TAB) ? 1f - offset : 0f;

                applyChrome(Math.max(0f, Math.min(1f, t)));
            }
        });
    }

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

    private void setupTabs() {
        cacheTabRipples();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < TAB_LABELS.length; i++) {
            View tab = inflater.inflate(R.layout.item_channel_tab, llTabs, false);
            ((TextView) tab.findViewById(R.id.tv_tab_label)).setText(TAB_LABELS[i]);
            final int index = i;
            tab.setOnClickListener(v -> selectTab(index));
            llTabs.addView(tab);
            tabViews.add(tab);
        }

        highlightTab(DEFAULT_TAB);
    }

    private void cacheTabRipples() {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);

        rippleLightRes = tv.resourceId;
    }

    private void selectTab(int index) {
        vpChannels.setCurrentItem(index, true);
    }

    private void highlightTab(int index) {
        currentTab = index;
        int selectedColor = overFeed ? R.color.text_on_dark : R.color.brand_red;
        int normalColor = overFeed ? R.color.text_on_dark_secondary : R.color.text_primary;
        for (int i = 0; i < tabViews.size(); i++) {
            boolean selected = i == index;
            TextView tv = tabViews.get(i).findViewById(R.id.tv_tab_label);
            View line = tabViews.get(i).findViewById(R.id.v_tab_line);
            tv.setTextColor(ContextCompat.getColor(requireContext(), selected ? selectedColor : normalColor));
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            line.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void scrollTabIntoView(int index) {
        View tab = tabViews.get(index);
        hsTabs.post(() -> hsTabs.smoothScrollTo(
                Math.max(0, (tab.getLeft() + tab.getRight()) / 2 - hsTabs.getWidth() / 2), 0));
    }

    private void applyChrome(float t) {

        topBar.setAlpha(1f - t);

        topBar.setVisibility(t >= 0.5f ? View.INVISIBLE : View.VISIBLE);

        vFeedScrim.setAlpha(Math.min(1f, t * 2f));

        boolean wantOverFeed = t >= 0.5f;
        if (wantOverFeed != overFeed) {
            overFeed = wantOverFeed;
            applyTabStyle();

            if (feedAdapter != null) feedAdapter.setActive(overFeed);
        }
    }

    private void applyTabStyle() {

        hsTabs.setBackgroundColor(overFeed ? Color.TRANSPARENT
                : ContextCompat.getColor(requireContext(), R.color.bg_surface));

        for (View tab : tabViews) {

            tab.setBackground(ripple(overFeed ? R.drawable.bg_ripple_on_dark : rippleLightRes));
        }

        highlightTab(currentTab);
    }

    private Drawable ripple(int resId) {
        return resId == 0 ? null : ContextCompat.getDrawable(requireContext(), resId);
    }

    private void measureChrome() {
        final View root = findViewById(R.id.root_video);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> applyChromePadding());
        llChrome.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> applyChromePadding());
    }

    private void applyChromePadding() {
        int padHorizontal = getResources().getDimensionPixelSize(R.dimen.space_m);
        channelAdapter.setGridPadding(llChrome.getHeight(), 0, padHorizontal);
    }

    @Override public void onSaveInstanceState(Bundle out) {
        out.putInt("channel", currentTab);
        super.onSaveInstanceState(out);
    }
    private void bindEvent() {
        findViewById(R.id.tv_search_hint).setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(requireContext());
            input.setHint("输入标题关键词");
            new android.app.AlertDialog.Builder(requireContext()).setTitle("搜索视频").setView(input)
                .setPositiveButton("搜索", (d,w) -> {
                    java.util.List<VideoItem> results = new java.util.ArrayList<>();
                    String key = input.getText().toString().trim();
                    for (java.util.List<VideoItem> group : channelData)
                        for (VideoItem item : group) if (item.getTitle().contains(key)) results.add(item);
                    if (results.isEmpty()) { Toast.makeText(requireContext(), "没有找到相关视频", Toast.LENGTH_SHORT).show(); return; }
                    String[] titles = new String[results.size()];
                    for (int i=0;i<titles.length;i++) titles[i]=results.get(i).getTitle();
                    new android.app.AlertDialog.Builder(requireContext()).setTitle("搜索结果")
                        .setItems(titles, (dialog,index) -> {
                            VideoItem item = results.get(index);
                            Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
                            intent.putExtra("title",item.getTitle()); intent.putExtra("source",item.getSource());
                            intent.putExtra("cover",item.getCoverRes()); intent.putExtra("desc",item.getDesc());
                            startActivity(intent);
                        }).setNegativeButton("关闭",null).show();
                }).setNegativeButton("取消",null).show();
        });
    }
}
