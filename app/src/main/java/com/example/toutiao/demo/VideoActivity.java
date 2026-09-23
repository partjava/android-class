package com.example.toutiao.demo;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频页，底部导航「视频」的落地页。
 *
 * 结构对照商城页：红顶栏 + 频道标签 + 列表 + 底部导航，
 * 区别是列表用单列大封面卡片流（LinearLayoutManager），
 * 不是商城那种两列瀑布流。
 *
 * · 频道标签：复用了商城页的 item_channel_tab 布局，选中带红下划线
 * · 数据：4 个频道各持有一份完整列表，切频道是整份换掉
 *   （不像商城那样从 allItems 里按分类过滤）
 * · 点击某条视频 → VideoDetailActivity 播放页
 */
public class VideoActivity extends AppCompatActivity {

    //频道标签，每个频道在 channelData 里对应一份同样下标的列表
    private static final String[] TAB_LABELS = {"推荐", "小视频", "影视", "音乐"};
    private static final int DEFAULT_TAB = 0;

    private HorizontalScrollView hsTabs;
    private LinearLayout llTabs;
    private RecyclerView rvVideo;
    private BottomNavigationView bottomNav;

    private final List<View> tabViews = new ArrayList<>();
    //每个频道各自的数据，下标和 TAB_LABELS 对齐
    private final List<List<VideoItem>> channelData = new ArrayList<>();

    //videoList 是"当前正在显示"的那一份，VideoAdapter 持有它的引用。
    //切频道时对这个对象做 clear + addAll，再 notifyDataSetChanged 就能刷新
    //（和首页切换新闻标签、商城切换品类是同一套路）
    private List<VideoItem> videoList;
    private VideoAdapter videoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        bindView();
        initVideoData();
        setupTabs();
        bindEvent();
    }

    private void bindView() {
        hsTabs = findViewById(R.id.hs_video_tabs);
        llTabs = findViewById(R.id.ll_video_tabs);
        rvVideo = findViewById(R.id.rv_video);
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void initVideoData() {
        channelData.add(buildRecommend());
        channelData.add(buildSmall());
        channelData.add(buildMovie());
        channelData.add(buildMusic());

        videoList = new ArrayList<>(channelData.get(DEFAULT_TAB));
        videoAdapter = new VideoAdapter(videoList);
        rvVideo.setLayoutManager(new LinearLayoutManager(this));
        rvVideo.setAdapter(videoAdapter);

        //点击某条视频进入播放页，带过去 6 个 extra
        videoAdapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(VideoActivity.this, VideoDetailActivity.class);
            intent.putExtra(VideoDetailActivity.EXTRA_TITLE, item.getTitle());
            intent.putExtra(VideoDetailActivity.EXTRA_SOURCE, item.getSource());
            intent.putExtra(VideoDetailActivity.EXTRA_DURATION, item.getDuration());
            intent.putExtra(VideoDetailActivity.EXTRA_PLAY_COUNT, item.getPlayCount());
            intent.putExtra(VideoDetailActivity.EXTRA_DESC, item.getDesc());
            intent.putExtra(VideoDetailActivity.EXTRA_COVER, item.getCoverRes());
            startActivity(intent);
        });
    }

    //===== 各频道的数据 =====
    /**
     * 封面的选图说明（重要）：
     *
     * 项目里**没有一张横构图的高清图**——image* 是分辨率最高的一组，
     * 但全是竖构图或近方图。封面框是 16:9（见 item_video.xml），
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
     * 每个频道内封面不重复；跨频道复用。
     */

    private List<VideoItem> buildRecommend() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("纪录片：大国重器是怎样炼成的", "央视新闻", "12:36", "128万次播放",
                "镜头走进重型装备制造车间，记录一台大型设备从零件加工到整机装配的全过程。工人师傅们在微米级的精度要求下，一遍遍测量、打磨。",
                R.drawable.image2));
        list.add(new VideoItem("延时摄影记录城市从黎明到黄昏", "影像志", "04:12", "56万次播放",
                "摄影师在城市最高处架设机位，连续拍摄十二个小时。整座城市的一天被压缩进短短几分钟。",
                R.drawable.image7));
        list.add(new VideoItem("航拍中国：从空中俯瞰大好河山", "航拍视角", "08:45", "92万次播放",
                "镜头掠过奔腾的江河、金黄的麦田和错落的村落，山川河流呈现出与地面完全不同的壮阔面貌。",
                R.drawable.image5));
        list.add(new VideoItem("实拍：消防员火场逆行救人全过程", "现场实录", "06:03", "213万次播放",
                "浓烟从窗口不断涌出。消防员沿着楼道逐层搜救，在五楼卧室内找到一名被困老人，背起老人沿原路撤离，整个过程不到六分钟。",
                R.drawable.img4));
        list.add(new VideoItem("街头采访：你理想中的生活是什么样的", "城市观察", "09:28", "34万次播放",
                "记者在街头随机采访了二十位路人。答案各不相同，但大多朴素而具体。",
                R.drawable.image8));
        return list;
    }

    private List<VideoItem> buildSmall() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("萌宠日常：猫咪第一次见到雪的反应", "萌宠日记", "00:15", "320万次播放",
                "第一次见到雪的猫咪先是愣在原地，随后小心翼翼地伸出一只爪子试探，最后还是忍不住扑了进去。",
                R.drawable.image5));
        list.add(new VideoItem("一分钟学会三道家常菜，新手零失败", "厨房小窍门", "00:58", "88万次播放",
                "西红柿炒蛋、蒜蓉青菜、可乐鸡翅，三步一步不落，照着做就行。",
                R.drawable.img4));
        list.add(new VideoItem("手艺人用木头做出会走的小机器人", "手工达人", "00:42", "156万次播放",
                "全程不用一颗钉子、一滴胶水，所有零件靠榫卯咬合。上紧发条后，机器人能稳稳走上好几米。",
                R.drawable.image7));
        list.add(new VideoItem("街头钢琴：路人即兴弹奏惊艳全场", "街头艺术", "00:33", "271万次播放",
                "琴键摆在广场中央，谁都可以坐下来弹一段。这位路人坐下后，围观的人群渐渐安静了。",
                R.drawable.image8));
        list.add(new VideoItem("慢镜头：雨天窗外的十分钟", "慢镜头", "00:21", "19万次播放",
                "雨滴砸在玻璃上，慢慢汇成一道道水痕。没有旁白，只有雨声。",
                R.drawable.image2));
        return list;
    }

    private List<VideoItem> buildMovie() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("话剧《茶馆》复排上演 幕后纪录", "戏剧观察", "15:20", "45万次播放",
                "本轮复排保留了经典版本的结构，同时在舞台呈现上做了新的尝试。镜头跟拍排练厅里的两个月。",
                R.drawable.image6));
        list.add(new VideoItem("电影幕后：一场雨戏是怎么拍出来的", "影视工业", "11:08", "67万次播放",
                "洒水车、灯光组、摄影机轨道，一场看起来只有两分钟的雨戏，背后是四十多人的配合。",
                R.drawable.image2));
        list.add(new VideoItem("老片修复：让经典重新清晰起来", "电影资料馆", "18:33", "28万次播放",
                "修复师逐帧处理划痕和噪点，一部两小时的老电影往往需要几个月才能完成。",
                R.drawable.image8));
        list.add(new VideoItem("配音演员现场：一个人配出整部剧", "声音工坊", "07:56", "103万次播放",
                "同一个人的声音在不同角色之间来回切换，录音棚外的观众听得目瞪口呆。",
                R.drawable.img4));
        list.add(new VideoItem("经典配乐赏析：旋律如何讲故事", "电影音乐", "13:14", "39万次播放",
                "从主题动机到配器选择，拆解几段耳熟能详的旋律是怎么把情绪推上去的。",
                R.drawable.image5));
        return list;
    }

    private List<VideoItem> buildMusic() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("国风音乐盛典：传统乐器演绎流行金曲", "音乐现场", "22:41", "74万次播放",
                "古筝、琵琶、笛箫与现代编曲结合，重新演绎了多首流行歌曲。熟悉的旋律换一种方式演奏，别有一番味道。",
                R.drawable.image7));
        list.add(new VideoItem("民谣现场：一把吉他唱完整个夏天", "livehouse", "05:37", "51万次播放",
                "小小的场地里挤满了人，唱到副歌时全场跟着一起哼。",
                R.drawable.image5));
        list.add(new VideoItem("交响乐团排练直击：指挥到底在说什么", "古典频道", "09:02", "22万次播放",
                "排练时指挥不断叫停，一遍遍调整某个声部的力度和进入时机。",
                R.drawable.image6));
        list.add(new VideoItem("草原上的长调：一个人的合唱", "民歌采集", "06:48", "18万次播放",
                "老人在空旷的草原上唱起长调，声音传得很远。录音师说，这种唱法已经很少有人会了。",
                R.drawable.image2));
        list.add(new VideoItem("深夜电台：城市入睡之后的声音", "声音志", "28:15", "63万次播放",
                "凌晨两点的便利店、收摊的夜市、最后一班公交，把这些声音拼在一起，就是一座城市的深夜。",
                R.drawable.image8));
        return list;
    }

    //==== 频道标签：和商城页同一套交互 ====

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

    //选中一个频道：高亮标签 + 把标签滚到屏幕中间 + 换列表数据
    private void selectTab(int index) {
        for (int i = 0; i < tabViews.size(); i++) {
            boolean selected = i == index;
            TextView tv = tabViews.get(i).findViewById(R.id.tv_tab_label);
            View line = tabViews.get(i).findViewById(R.id.v_tab_line);
            tv.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.brand_red : R.color.text_primary));
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            line.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
        //把选中的标签滚到可视区域中间。post() 是等测量完成后宽度才有值
        View tab = tabViews.get(index);
        hsTabs.post(() -> hsTabs.smoothScrollTo(
                Math.max(0, (tab.getLeft() + tab.getRight()) / 2 - hsTabs.getWidth() / 2), 0));

        //换数据：clear + addAll，adapter 持有同一个引用
        videoList.clear();
        videoList.addAll(channelData.get(index));
        videoAdapter.notifyDataSetChanged();
        rvVideo.scrollToPosition(0);
    }

    //==== 搜索入口 + 底部导航 ====

    private void bindEvent() {
        //顶栏搜索入口只是个占位，不做真正的搜索
        TextView tvSearchHint = findViewById(R.id.tv_search_hint);
        tvSearchHint.setOnClickListener(v ->
                Toast.makeText(VideoActivity.this, "搜索功能仅为演示", Toast.LENGTH_SHORT).show());

        //==== 底部导航：写法和首页/商城/我的页保持一致 ====
        bottomNav.setSelectedItemId(R.id.nav_video);
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
