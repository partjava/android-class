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
    private VideoAlbum album;
    private VideoStore videoStore;
    private View rlCover;
    private FrameLayout flProgress;
    private View vProgress;
    private ImageView ivCover, ivPlay, ivLike, ivCollect;
    private TextView tvDuration, tvTitle, tvMeta, tvDesc, tvLikeCount, tvCollect;
    private LinearLayout llRelated, llDetailAlbum;
    private View llDetailLike, llDetailCollect;

    /** 收藏态读取走单线程 executor，onCreate/bindData 异步刷新按钮图标。 */
    private final java.util.concurrent.ExecutorService stateExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

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
        setupGestureControls();

        findViewById(R.id.iv_detail_back).setOnClickListener(v -> finish());
    }

    private void setupGestureControls() {
        android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                togglePlay();
                return true;
            }

            @Override
            public boolean onDoubleTap(android.view.MotionEvent e) {
                showDoubleTapLikeHeart(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onScroll(android.view.MotionEvent e1, android.view.MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null) return false;
                int viewWidth = rlCover.getWidth();
                int viewHeight = rlCover.getHeight();
                if (viewWidth <= 0 || viewHeight <= 0) return false;

                if (Math.abs(distanceY) > Math.abs(distanceX)) {
                    if (e1.getX() < viewWidth / 2.0f) {
                        adjustBrightness(distanceY / viewHeight);
                    } else {
                        adjustVolume(distanceY / viewHeight);
                    }
                    return true;
                }
                return false;
            }
        });

        rlCover.setOnTouchListener((v, event) -> {
            boolean handled = gestureDetector.onTouchEvent(event);
            if (!handled && event.getAction() == android.view.MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
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
        videoStore = new VideoStore(this);
        ivLike = findViewById(R.id.iv_detail_like);
        ivCollect = findViewById(R.id.iv_detail_collect);
        tvLikeCount = findViewById(R.id.tv_detail_like_count);
        tvCollect = findViewById(R.id.tv_detail_collect);
        llDetailLike = findViewById(R.id.ll_detail_like);
        llDetailCollect = findViewById(R.id.ll_detail_collect);
        llDetailAlbum = findViewById(R.id.ll_detail_album);
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
        //与 NewsDetailActivity 一致：列表页不携带正文，进详情页才按标题查询
        if (desc == null || desc.isEmpty()) {
            desc = NewsContentStore.contentOf(title);
        }
        int cover = intent.getIntExtra(EXTRA_COVER, 0);

        tvTitle.setText(title == null || title.isEmpty() ? "视频" : title);

        if(cover==0) cover=R.drawable.image7;
        // 优先命中目录里的完整 mock 数据（时长/播放量/点赞数），未收录的视频（如新闻流视频）走 intent 兜底
        VideoItem catalogItem = VideoCatalog.get(this).byTitle(title);
        currentItem = catalogItem != null ? catalogItem
                : new VideoItem(title==null?"光影练习":title,"离线影像课堂","00:08","本地样片",desc==null?"离线播放器课程演示":desc,cover,0,0);
        tvTitle.setText(currentItem.getTitle());
        album = VideoCatalog.get(this).albumOf(currentItem);
        org.json.JSONObject snapshot = NewsContract.snapshot(intent);
        try {
            snapshot.put("detail_type",NewsContract.VIDEO); snapshot.put("content",desc); snapshot.put("info",source); snapshot.put("img",cover);
            // 覆盖 LegacyIds 默认 id：视频快照与 feed 收藏共用 video- 前缀的稳定 id
            snapshot.put("news_id", NewsContract.videoNewsId(currentItem.getTitle()));
            snapshot.put("duration", currentItem.getDuration());
            snapshot.put("play_count", currentItem.getPlayCount());
            snapshot.put("album_id", album == null ? "" : album.getId());
        } catch(Exception ignored) {}
        ContentStore store = new ContentStore(this);
        store.put("history",snapshot);
        ivCover.setImageResource(cover);
        player.setResource(currentItem.getVideoRes());
        tvDuration.setVisibility(View.VISIBLE);
        tvDuration.setText("课程样片 · " + currentItem.getDuration());
        // 副标题：来源 · 简介，独立视频与专辑视频共用同一版式
        tvMeta.setText(currentItem.getSource() + " · " + (album != null ? album.getSubtitle() : "无声动画样片"));
        tvMeta.setVisibility(View.VISIBLE);
        tvDesc.setText(currentItem.getDesc());
        renderAlbumCard();
        renderActionsAsync();
        bindActionClicks(store);
        play();

        fillRelated();
    }

    /** 点赞/收藏操作行：收藏写 SQLite（与 feed、我的页同源），点赞写 VideoStore。 */
    private void bindActionClicks(final ContentStore store) {
        llDetailLike.setOnClickListener(v -> {
            boolean liked = !videoStore.isLiked(currentItem.getId());
            videoStore.setLiked(currentItem.getId(), liked);
            renderLikeState(liked);
        });
        llDetailCollect.setOnClickListener(v -> {
            String key = NewsContract.videoNewsId(currentItem.getTitle());
            boolean saved;
            if (store.contains("saved", key)) {
                store.remove("saved", key);
                saved = false;
            } else {
                store.put("saved", NewsContract.videoSnapshot(currentItem, album == null ? "" : album.getId()));
                saved = true;
            }
            renderCollectState(saved);
        });
    }

    /** onCreate/bindData 异步读取持久化收藏态，动态更新按钮图标。 */
    private void renderActionsAsync() {
        final String key = NewsContract.videoNewsId(currentItem.getTitle());
        final boolean liked = videoStore.isLiked(currentItem.getId());
        stateExecutor.execute(() -> {
            boolean collected = new ContentStore(VideoDetailActivity.this).contains("saved", key);
            if (isDestroyed() || isFinishing()) return;
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                renderLikeState(liked);
                renderCollectState(collected);
            });
        });
    }

    private void renderLikeState(boolean liked) {
        ivLike.setColorFilter(androidx.core.content.ContextCompat.getColor(this,
                liked ? R.color.brand_red : R.color.text_on_dark));
        tvLikeCount.setText(formatLikeCount(currentItem.getLikeCount() + (liked ? 1 : 0)));
        llDetailLike.setContentDescription(liked ? "取消点赞" : "点赞");
    }

    private void renderCollectState(boolean saved) {
        ivCollect.setColorFilter(androidx.core.content.ContextCompat.getColor(this,
                saved ? R.color.collect_gold : R.color.text_on_dark));
        tvCollect.setText(saved ? "已收藏" : "收藏");
        llDetailCollect.setContentDescription(saved ? "取消收藏" : "收藏");
    }

    private String formatLikeCount(int count) {
        return count >= 10000
                ? String.format(java.util.Locale.CHINA, "%.1f万", count / 10000.0)
                : String.valueOf(count);
    }

    /** 专辑归属卡：视频属于某专辑时展示，点击进入专辑页。幂等，onNewIntent 复用安全。 */
    private void renderAlbumCard() {
        llDetailAlbum.removeAllViews();
        if (album == null) {
            llDetailAlbum.setVisibility(View.GONE);
            return;
        }
        llDetailAlbum.setVisibility(View.VISIBLE);
        View card = LayoutInflater.from(this).inflate(R.layout.item_detail_album_card, llDetailAlbum, false);
        ((ImageView) card.findViewById(R.id.iv_album_cover)).setImageResource(album.getCoverRes());
        ((TextView) card.findViewById(R.id.tv_album_card_title)).setText(album.getTitle());
        ((TextView) card.findViewById(R.id.tv_album_card_subtitle))
                .setText("专辑 · 共" + album.getCount() + "集 · " + album.getUpdateInfo());
        card.setOnClickListener(v -> AlbumActivity.start(this, album.getId()));
        llDetailAlbum.addView(card);
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
        // 专辑视频：相关推荐换成本专辑的其余各集；独立视频维持原有推荐
        if (album != null) {
            for (VideoItem episode : VideoCatalog.get(this).albumVideos(album.getId()))
                if (!episode.getTitle().equals(currentItem.getTitle())) list.add(episode);
            if (!list.isEmpty()) return list;
        }
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
        stateExecutor.shutdownNow();
    }

    private void showDoubleTapLikeHeart(float x, float y) {
        // 双击即点赞：与操作行按钮写同一份 VideoStore 状态
        if (currentItem != null) {
            videoStore.setLiked(currentItem.getId(), true);
            renderLikeState(true);
        }
        ImageView heart = new ImageView(this);
        heart.setImageResource(R.drawable.ic_like);
        heart.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.brand_red));
        int size = dpToPx(72);
        android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(size, size);
        heart.setLayoutParams(lp);
        heart.setX(x - size / 2.0f);
        heart.setY(y - size / 2.0f);
        heart.setRotation(-15f + (float) (Math.random() * 30f));
        heart.setScaleX(0.3f);
        heart.setScaleY(0.3f);
        heart.setAlpha(0.9f);

        ((ViewGroup) rlCover).addView(heart);

        heart.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .alpha(1.0f)
                .setDuration(200)
                .withEndAction(() -> heart.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationYBy(-dpToPx(60))
                        .alpha(0.0f)
                        .setDuration(350)
                        .withEndAction(() -> ((ViewGroup) rlCover).removeView(heart))
                        .start())
                .start();

        showGestureTip("❤️ 双击点赞！");
    }

    private void adjustBrightness(float percent) {
        android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
        float current = lp.screenBrightness;
        if (current < 0) current = 0.5f;
        current += percent;
        if (current > 1.0f) current = 1.0f;
        if (current < 0.05f) current = 0.05f;
        lp.screenBrightness = current;
        getWindow().setAttributes(lp);
        int brightnessPercent = (int) (current * 100);
        showGestureTip("🔆 亮度：" + brightnessPercent + "%");
    }

    private void adjustVolume(float percent) {
        android.media.AudioManager am = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
        if (am != null) {
            int maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            int currentVol = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            int delta = (int) (percent * maxVol * 1.5f);
            if (delta == 0 && Math.abs(percent) > 0.01f) {
                delta = percent > 0 ? 1 : -1;
            }
            int newVol = Math.max(0, Math.min(maxVol, currentVol + delta));
            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0);
            int volPercent = (int) ((newVol * 100.0f) / maxVol);
            showGestureTip("🔊 音量：" + volPercent + "%");
        }
    }

    private android.widget.Toast gestureToast;

    private void showGestureTip(String msg) {
        if (gestureToast != null) gestureToast.cancel();
        gestureToast = android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT);
        gestureToast.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
