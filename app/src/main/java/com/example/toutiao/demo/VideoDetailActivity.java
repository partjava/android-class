package com.example.toutiao.demo;

import android.content.Intent;
import android.graphics.Color;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.ArrayList;
import java.util.List;

public class VideoDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_PLAY_COUNT = "play_count";
    public static final String EXTRA_DESC = "desc";
    public static final String EXTRA_COVER = "cover";

    private static final long TICK_MS = 200L;
    private static final int PROGRESS_MAX = 100;

    private OfflinePlayer player;
    private VideoItem currentItem;
    private View rlCover;
    private FrameLayout flProgress;
    private View vProgress;
    private ImageView ivCover, ivPlay;
    private TextView tvDuration, tvTitle, tvMeta, tvDesc;
    private LinearLayout llRelated;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int progress = 0;
    private boolean playing = false;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!playing) return;
            progress=player.progress();
            renderProgress();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupImmersive();
        setContentView(R.layout.activity_video_detail);
        bindView();
        bindData();
        applyWindowInsets();

        rlCover.setOnClickListener(v -> togglePlay());
        findViewById(R.id.iv_detail_back).setOnClickListener(v -> finish());
    }

    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bindData();
    }

    private void bindView() {
        player = findViewById(R.id.player_detail);
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

    
    private void setupImmersive() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars());

            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            controller.setAppearanceLightStatusBars(false);
        }

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        getWindow().setBackgroundDrawableResource(R.color.bg_player);
    }

    
    private void applyWindowInsets() {
        final View root = findViewById(R.id.root_detail);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(0, bars.top, 0, bars.bottom);

            return insets;
        });
    }

    private void bindData() {

        resetPlayback();

        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_TITLE);
        String source = intent.getStringExtra(EXTRA_SOURCE);
        String duration = intent.getStringExtra(EXTRA_DURATION);
        String playCount = intent.getStringExtra(EXTRA_PLAY_COUNT);
        String desc = intent.getStringExtra(EXTRA_DESC);
        int cover = intent.getIntExtra(EXTRA_COVER, 0);

        tvTitle.setText(title == null || title.isEmpty() ? "视频" : title);

        if(cover==0) cover=R.drawable.image7;
        currentItem=new VideoItem(title==null?"光影练习":title,"离线影像课堂","00:08","本地样片",desc==null?"离线播放器课程演示":desc,cover,0,0);
        tvTitle.setText(currentItem.getTitle());
        ivCover.setImageResource(cover);
        player.setResource(currentItem.getVideoRes());
        tvDuration.setVisibility(View.VISIBLE);
        tvDuration.setText("课程样片 · 00:08");
        tvMeta.setText("离线影像课堂 · 无声动画样片");
        tvMeta.setVisibility(View.VISIBLE);
        tvDesc.setText(currentItem.getDesc());
        play();

        fillRelated();
    }

    private void togglePlay() {
        if (playing) {
            pause();
        } else {
            play();
        }
    }

    private void play() {
        player.play();

        if (progress >= PROGRESS_MAX) {
            progress = 0;
            renderProgress();
        }
        playing = true;
        ivPlay.setImageResource(R.drawable.ic_pause_circle);
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, TICK_MS);
    }

    private void pause() {
        player.pause();
        playing = false;
        ivPlay.setImageResource(R.drawable.ic_play_circle);
        handler.removeCallbacks(ticker);
    }

    private void renderProgress() {
        int trackWidth = flProgress.getWidth();

        if (trackWidth <= 0) {
            return;
        }
        ViewGroup.LayoutParams lp = vProgress.getLayoutParams();
        lp.width = trackWidth * progress / PROGRESS_MAX;
        vProgress.setLayoutParams(lp);
    }

    private void resetPlayback() {
        if(player!=null) player.release();
        handler.removeCallbacks(ticker);
        playing = false;
        progress = 0;
        ivPlay.setImageResource(R.drawable.ic_play_circle);

        flProgress.post(this::renderProgress);
    }

    
    private List<VideoItem> buildRelated() {
        List<VideoItem> list = new ArrayList<>();
        list.add(new VideoItem("延时摄影记录城市从黎明到黄昏", "影像志", "04:12", "56万次播放",
                "摄影师在城市最高处架设机位，连续拍摄十二个小时。整座城市的一天被压缩进短短几分钟。",
                R.drawable.image7, 3200, 168));
        list.add(new VideoItem("航拍中国：从空中俯瞰大好河山", "航拍视角", "08:45", "92万次播放",
                "镜头掠过奔腾的江河、金黄的麦田和错落的村落，山川河流呈现出与地面完全不同的壮阔面貌。",
                R.drawable.image5, 5400, 271));
        list.add(new VideoItem("街头采访：你理想中的生活是什么样的", "城市观察", "09:28", "34万次播放",
                "记者在街头随机采访了二十位路人。答案各不相同，但大多朴素而具体。",
                R.drawable.image8, 2100, 486));
        list.add(new VideoItem("民谣现场：一把吉他唱完整个夏天", "livehouse", "05:37", "51万次播放",
                "小小的场地里挤满了人，唱到副歌时全场跟着一起哼。",
                R.drawable.image6, 2900, 174));
        return list;
    }

    private void fillRelated() {
        llRelated.removeAllViews();
        String currentTitle = getIntent().getStringExtra(EXTRA_TITLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (VideoItem item : buildRelated()) {

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

    private void openVideo(VideoItem item) {
        Intent intent = new Intent(VideoDetailActivity.this, VideoDetailActivity.class);
        intent.putExtra(EXTRA_TITLE, item.getTitle());
        intent.putExtra(EXTRA_SOURCE, item.getSource());
        intent.putExtra(EXTRA_DURATION, item.getDuration());
        intent.putExtra(EXTRA_PLAY_COUNT, item.getPlayCount());
        intent.putExtra(EXTRA_DESC, item.getDesc());
        intent.putExtra(EXTRA_COVER, item.getCoverRes());

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    @Override protected void onPause(){pause();player.release();super.onPause();}
    @Override protected void onResume(){super.onResume();if(player!=null&&currentItem!=null)play();}
    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
        player.release();
    }
}
