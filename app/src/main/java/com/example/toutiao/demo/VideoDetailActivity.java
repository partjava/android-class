package com.example.toutiao.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频播放页。
 *
 * 两个入口，能提供的数据不一样，所以 extra 是分档的：
 *
 *   入口                     提供
 *   ─────────────────────   ────────────────────────────────
 *   视频页（VideoActivity）   标题/来源/时长/播放量/简介/封面
 *   首页「视频」标签          标题/来源/简介/封面
 *                            （News 模型里没有时长和播放量）
 *
 * 取不到的字段**直接把控件藏掉，不编假数据**——少一个控件比多一个假数字诚实。
 *
 * 播放是模拟的：点播放键每 200ms 推进 2%，十秒走满，走满停在 100% 不循环。
 * 没有接 MediaPlayer，全项目本来就不联网、也没有视频文件。
 */
public class VideoDetailActivity extends AppCompatActivity {

    //extra 的 key。VideoActivity 和 HomeActivity 都引用这里，
    //避免两边各写一份字符串对不上
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_PLAY_COUNT = "play_count";
    public static final String EXTRA_DESC = "desc";
    public static final String EXTRA_COVER = "cover";

    //模拟播放：200ms 一跳、每跳 2%，约 10 秒走满
    private static final long TICK_MS = 200L;
    private static final int TICK_STEP = 2;
    private static final int PROGRESS_MAX = 100;

    private View rlCover;
    private FrameLayout flProgress;
    private View vProgress;
    private ImageView ivCover, ivPlay;
    private TextView tvDuration, tvTitle, tvMeta, tvDesc;
    private LinearLayout llRelated;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int progress = 0;
    private boolean playing = false;

    //每 200ms 推进一次进度。用字段而不是匿名类，是为了能按同一个实例
    //removeCallbacks —— 每次 new 一个 Runnable 是移除不掉的
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            progress += TICK_STEP;
            if (progress >= PROGRESS_MAX) {
                progress = PROGRESS_MAX;
                renderProgress();
                //走满停在满格，不循环回 0
                pause();
                return;
            }
            renderProgress();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);
        bindView();
        bindData();

        //整个封面区都是播放/暂停的点击区，不是只有中间那个 48dp 的键——
        //和真实播放器一致，点起来也不用瞄准
        rlCover.setOnClickListener(v -> togglePlay());
        findViewById(R.id.iv_detail_back).setOnClickListener(v -> finish());
    }

    /**
     * 点「相关视频」跳过来时会走这里，不是 onCreate。
     *
     * 因为跳转带的是 FLAG_ACTIVITY_SINGLE_TOP | CLEAR_TOP，栈里这个实例
     * 会被直接复用。不重写这个方法的话，页面标题、封面、进度条全都还是
     * 上一条视频的——看着就像"点了没反应"。
     *
     * setIntent 不能省：不换掉的话下面 bindData() 里的 getIntent()
     * 拿到的还是最初那个 Intent。
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bindData();
    }

    private void bindView() {
        rlCover = findViewById(R.id.rl_detail_cover);
        flProgress = findViewById(R.id.fl_detail_progress);
        vProgress = findViewById(R.id.v_detail_progress);
        ivCover = findViewById(R.id.iv_detail_cover);
        ivPlay = findViewById(R.id.iv_detail_play);
        tvDuration = findViewById(R.id.tv_detail_duration);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvMeta = findViewById(R.id.tv_detail_meta);
        tvDesc = findViewById(R.id.tv_detail_desc);
        llRelated = findViewById(R.id.ll_related);
    }

    //把当前 Intent 里的数据铺到页面上。onCreate 和 onNewIntent 都走这里
    private void bindData() {
        //换视频时先把播放状态归零，否则新视频一进来进度条就是满的
        resetPlayback();

        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_TITLE);
        String source = intent.getStringExtra(EXTRA_SOURCE);
        String duration = intent.getStringExtra(EXTRA_DURATION);
        String playCount = intent.getStringExtra(EXTRA_PLAY_COUNT);
        String desc = intent.getStringExtra(EXTRA_DESC);
        int cover = intent.getIntExtra(EXTRA_COVER, 0);

        //兜底：万一没带标题进来，页面也不至于是空的
        tvTitle.setText(title == null || title.isEmpty() ? "视频" : title);

        //封面。取不到就把整块（含播放键）藏掉，不留一块 203dp 的空白
        if (cover != 0) {
            rlCover.setVisibility(View.VISIBLE);
            ivCover.setImageResource(cover);
        } else {
            rlCover.setVisibility(View.GONE);
        }

        //时长。首页视频标签进来的没有这个数据，藏掉而不是编一个
        if (duration == null || duration.isEmpty()) {
            tvDuration.setVisibility(View.GONE);
        } else {
            tvDuration.setVisibility(View.VISIBLE);
            tvDuration.setText(duration);
        }

        //「来源 · 播放量」：两边都可能缺，缺谁不显示谁；都没有就整行藏掉
        StringBuilder meta = new StringBuilder();
        if (source != null && !source.isEmpty()) {
            meta.append(source);
        }
        if (playCount != null && !playCount.isEmpty()) {
            if (meta.length() > 0) {
                meta.append(" · ");
            }
            meta.append(playCount);
        }
        if (meta.length() == 0) {
            tvMeta.setVisibility(View.GONE);
        } else {
            tvMeta.setVisibility(View.VISIBLE);
            tvMeta.setText(meta.toString());
        }

        //简介。没写的话兜一段，和 NewsDetailActivity 的处理一致
        if (desc == null || desc.isEmpty()) {
            desc = "　　" + tvTitle.getText()
                    + "\n\n　　（本条为课程演示数据，完整视频略。）";
        }
        tvDesc.setText(desc);

        fillRelated();
    }

    //==== 模拟播放 ====

    private void togglePlay() {
        if (playing) {
            pause();
        } else {
            play();
        }
    }

    private void play() {
        //已经播完了，再点就从头开始
        if (progress >= PROGRESS_MAX) {
            progress = 0;
            renderProgress();
        }
        playing = true;
        ivPlay.setImageResource(R.drawable.ic_pause_circle);
        handler.postDelayed(ticker, TICK_MS);
    }

    private void pause() {
        playing = false;
        ivPlay.setImageResource(R.drawable.ic_play_circle);
        handler.removeCallbacks(ticker);
    }

    //把已经播放的比例画出来：直接改填充条的宽度
    private void renderProgress() {
        int trackWidth = flProgress.getWidth();
        //还没测量完（宽度为 0）就先跳过，下一跳会补上
        if (trackWidth <= 0) {
            return;
        }
        ViewGroup.LayoutParams lp = vProgress.getLayoutParams();
        lp.width = trackWidth * progress / PROGRESS_MAX;
        vProgress.setLayoutParams(lp);
    }

    private void resetPlayback() {
        handler.removeCallbacks(ticker);
        playing = false;
        progress = 0;
        ivPlay.setImageResource(R.drawable.ic_play_circle);
        //布局还没测量完时宽度取不到，post 一下等它测完再画
        flProgress.post(this::renderProgress);
    }

    //==== 相关视频 ====

    /**
     * 相关视频的候选。固定 4 条，填的时候会把「当前正在播的这条」剔掉，
     * 所以实际显示 3~4 条。
     *
     * 封面仍然只在 16:9 裁得动的那几张里选（见 VideoActivity 里的选图说明），
     * 4 条互不重复。
     */
    private List<VideoItem> buildRelated() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("延时摄影记录城市从黎明到黄昏", "影像志", "04:12", "56万次播放",
                "摄影师在城市最高处架设机位，连续拍摄十二个小时。整座城市的一天被压缩进短短几分钟。",
                R.drawable.image7));
        list.add(new VideoItem("航拍中国：从空中俯瞰大好河山", "航拍视角", "08:45", "92万次播放",
                "镜头掠过奔腾的江河、金黄的麦田和错落的村落，山川河流呈现出与地面完全不同的壮阔面貌。",
                R.drawable.image5));
        list.add(new VideoItem("街头采访：你理想中的生活是什么样的", "城市观察", "09:28", "34万次播放",
                "记者在街头随机采访了二十位路人。答案各不相同，但大多朴素而具体。",
                R.drawable.image8));
        list.add(new VideoItem("民谣现场：一把吉他唱完整个夏天", "livehouse", "05:37", "51万次播放",
                "小小的场地里挤满了人，唱到副歌时全场跟着一起哼。",
                R.drawable.image6));
        return list;
    }

    private void fillRelated() {
        llRelated.removeAllViews();
        String currentTitle = getIntent().getStringExtra(EXTRA_TITLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (VideoItem item : buildRelated()) {
            //别把「当前这条」列进相关推荐里
            if (item.getTitle().equals(currentTitle)) {
                continue;
            }
            View row = inflater.inflate(R.layout.item_video_related, llRelated, false);
            ((ImageView) row.findViewById(R.id.iv_related_cover))
                    .setImageResource(item.getCoverRes());
            ((TextView) row.findViewById(R.id.tv_related_title)).setText(item.getTitle());
            ((TextView) row.findViewById(R.id.tv_related_source)).setText(item.getSource());
            row.setOnClickListener(v -> openVideo(item));
            llRelated.addView(row);
        }
    }

    //点相关视频：复用当前这个实例换内容，不往栈里堆新页面
    private void openVideo(VideoItem item) {
        Intent intent = new Intent(VideoDetailActivity.this, VideoDetailActivity.class);
        intent.putExtra(EXTRA_TITLE, item.getTitle());
        intent.putExtra(EXTRA_SOURCE, item.getSource());
        intent.putExtra(EXTRA_DURATION, item.getDuration());
        intent.putExtra(EXTRA_PLAY_COUNT, item.getPlayCount());
        intent.putExtra(EXTRA_DESC, item.getDesc());
        intent.putExtra(EXTRA_COVER, item.getCoverRes());
        //和底部导航用的是同一套栈管理策略：CLEAR_TOP + SINGLE_TOP
        //会复用栈里已有的本页实例（走 onNewIntent），反复点也不会堆栈
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //把还在跑的进度定时器清掉。不清的话用户点完播放立刻退出，
        //ticker 仍会每 200ms 执行一次，而它持有这个 Activity 的引用，
        //Activity 就没法被回收了（和 ChatActivity 的模拟回复同一个坑）
        handler.removeCallbacksAndMessages(null);
    }
}
