package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 专辑（专题）页：展示专辑信息与全部视频，可整体收藏，浏览进入写浏览记录。 */
public class AlbumActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_ID = "album_id";

    private VideoAlbum album;
    private ContentStore store;

    private ImageView ivHeader, ivCollect;
    private TextView tvTitle, tvSubtitle, tvCollect;
    private LinearLayout llEpisodes;

    /** 收藏态异步读取，与详情页同一模式。 */
    private final ExecutorService stateExecutor = Executors.newSingleThreadExecutor();

    public static void start(Context context, String albumId) {
        context.startActivity(new Intent(context, AlbumActivity.class).putExtra(EXTRA_ALBUM_ID, albumId));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setBackgroundDrawableResource(R.color.bg_player);
        setContentView(R.layout.activity_album);
        applyWindowInsets();

        store = new ContentStore(this);

        album = VideoCatalog.get(this).albumById(getIntent().getStringExtra(EXTRA_ALBUM_ID));
        if (album == null) {
            Toast.makeText(this, "专辑不存在或已下线", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        bindHeader();
        renderEpisodes();
        writeAlbumHistory();
        loadCollectStateAsync();
    }

    private void applyWindowInsets() {
        final View root = findViewById(R.id.root_album);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        ivHeader = findViewById(R.id.iv_album_header);
        ivCollect = findViewById(R.id.iv_album_collect);
        tvTitle = findViewById(R.id.tv_album_title);
        tvSubtitle = findViewById(R.id.tv_album_subtitle);
        tvCollect = findViewById(R.id.tv_album_collect);
        llEpisodes = findViewById(R.id.ll_album_episodes);
        findViewById(R.id.iv_album_back).setOnClickListener(v -> finish());
        findViewById(R.id.ll_album_collect).setOnClickListener(v -> toggleCollect());
    }

    private void bindHeader() {
        ivHeader.setImageResource(album.getCoverRes());
        tvTitle.setText(album.getTitle());
        tvSubtitle.setText(album.getSubtitle() + " · 共" + album.getCount() + "集 · " + album.getUpdateInfo());
    }

    private void renderEpisodes() {
        llEpisodes.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        List<VideoItem> episodes = VideoCatalog.get(this).albumVideos(album.getId());
        for (int i = 0; i < episodes.size(); i++) {
            final VideoItem item = episodes.get(i);
            View row = inflater.inflate(R.layout.item_video_related, llEpisodes, false);
            ((ImageView) row.findViewById(R.id.iv_related_cover)).setImageResource(item.getCoverRes());
            ((TextView) row.findViewById(R.id.tv_related_title))
                    .setText((i + 1) + " · " + item.getTitle());
            ((TextView) row.findViewById(R.id.tv_related_source))
                    .setText(item.getSource() + " · " + item.getDuration());
            row.setOnClickListener(v -> openEpisode(item));
            llEpisodes.addView(row);
        }
    }

    private void openEpisode(VideoItem item) {
        Intent intent = new Intent(this, VideoDetailActivity.class);
        intent.putExtra(VideoDetailActivity.EXTRA_TITLE, item.getTitle());
        intent.putExtra(VideoDetailActivity.EXTRA_SOURCE, item.getSource());
        intent.putExtra(VideoDetailActivity.EXTRA_DURATION, item.getDuration());
        intent.putExtra(VideoDetailActivity.EXTRA_PLAY_COUNT, item.getPlayCount());
        intent.putExtra(VideoDetailActivity.EXTRA_DESC, item.getDesc());
        intent.putExtra(VideoDetailActivity.EXTRA_COVER, item.getCoverRes());
        // 播放页互相跳转沿用 CLEAR_TOP|SINGLE_TOP，避免返回栈堆叠
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    /** 打开专辑即写一条专辑浏览记录（SQLite），浏览历史里可回看。 */
    private void writeAlbumHistory() {
        store.put("history", NewsContract.albumSnapshot(album));
    }

    private String collectKey() {
        return "album-" + album.getId();
    }

    private void loadCollectStateAsync() {
        stateExecutor.execute(() -> {
            boolean saved = store.contains("saved", collectKey());
            if (isDestroyed() || isFinishing()) return;
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                renderCollect(saved);
            });
        });
    }

    private void toggleCollect() {
        boolean saved;
        if (store.contains("saved", collectKey())) {
            store.remove("saved", collectKey());
            saved = false;
        } else {
            store.put("saved", NewsContract.albumSnapshot(album));
            saved = true;
        }
        renderCollect(saved);
    }

    private void renderCollect(boolean saved) {
        ivCollect.setColorFilter(androidx.core.content.ContextCompat.getColor(this,
                saved ? R.color.collect_gold : R.color.text_on_dark));
        tvCollect.setText(saved ? "已收藏专辑" : "收藏专辑");
        findViewById(R.id.ll_album_collect).setContentDescription(saved ? "取消收藏专辑" : "收藏专辑");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stateExecutor.shutdownNow();
    }
}
