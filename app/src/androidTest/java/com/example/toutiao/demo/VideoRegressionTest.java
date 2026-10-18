package com.example.toutiao.demo;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VideoRegressionTest {
    @Test public void legacyCollectionsMigrateOnceAndStayRemoved() throws Exception {
        Context base = ApplicationProvider.getApplicationContext();
        String suffix = "video-test-" + java.util.UUID.randomUUID();
        Context isolated = new ContextWrapper(base) {
            @Override public Context getApplicationContext() { return this; }
            @Override public android.content.SharedPreferences getSharedPreferences(String name, int mode) { return base.getSharedPreferences(suffix + name, mode); }
            @Override public File getDatabasePath(String name) { return new File(base.getCacheDir(), suffix + name); }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) { return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory); }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, android.database.DatabaseErrorHandler handler) { return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(), factory, handler); }
        };
        VideoItem item = new VideoItem("纪录片：大国重器是怎样炼成的", "", "", "", "", R.drawable.image2, 0, 0);
        new VideoStore(isolated).setCollected(item.getId(), true);
        java.lang.reflect.Constructor<VideoCatalog> constructor = VideoCatalog.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        constructor.newInstance(isolated);
        ContentStore content = new ContentStore(isolated);
        String key = NewsContract.videoNewsId(item.getTitle());
        assertTrue("Old feed collection must appear in SQLite", content.contains("saved", key));
        content.remove("saved", key);
        constructor.newInstance(isolated);
        assertFalse("Cancelled collection must not be imported again", content.contains("saved", key));
    }

    @Test public void likingDuringPendingReadStillRefreshesCollection() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String title = "pending-reaction-" + java.util.UUID.randomUUID();
        ContentStore content = new ContentStore(context);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        android.content.Intent intent = new android.content.Intent(context, VideoDetailActivity.class);
        intent.putExtra(VideoDetailActivity.EXTRA_TITLE, title);
        try (ActivityScenario<VideoDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field field = VideoDetailActivity.class.getDeclaredField("stateExecutor");
                    field.setAccessible(true);
                    ((java.util.concurrent.ExecutorService) field.get(activity)).execute(() -> {
                        try { release.await(5, java.util.concurrent.TimeUnit.SECONDS); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    });
                    VideoItem item = new VideoItem(title, "", "", "", "", R.drawable.image2, 0, 0);
                    content.put("saved", NewsContract.videoSnapshot(item, ""));
                    java.lang.reflect.Method refresh = VideoDetailActivity.class.getDeclaredMethod("renderActionsAsync");
                    refresh.setAccessible(true);
                    refresh.invoke(activity);
                    activity.findViewById(R.id.ll_detail_like).performClick();
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            release.countDown();
            AtomicReference<String> state = new AtomicReference<>();
            for (int i = 0; i < 50; i++) {
                scenario.onActivity(activity -> state.set(String.valueOf(activity.findViewById(R.id.ll_detail_collect).getContentDescription())));
                if ("取消收藏".equals(state.get())) break;
                Thread.sleep(100);
            }
            assertEquals("取消收藏", state.get());
        } finally {
            release.countDown();
            content.remove("saved", NewsContract.videoNewsId(title));
            new VideoStore(context).setLiked(new VideoItem(title, "", "", "", "", R.drawable.image2, 0, 0).getId(), false);
        }
    }

    @Test public void visibleFeedReactionsRefreshOnResume() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        VideoItem item = new VideoItem("reaction-test-" + java.util.UUID.randomUUID(), "", "", "", "", R.drawable.image2, 0, 0);
        VideoStore reactions = new VideoStore(context);
        ContentStore content = new ContentStore(context);
        AtomicReference<VideoFeedAdapter> adapterRef = new AtomicReference<>();
        AtomicReference<RecyclerView> recycler = new AtomicReference<>();
        ShopItem host = new ShopItem("feed-test-host", "测试", 1, 0, R.drawable.ic_shop, "其他", "", "{}");
        try (ActivityScenario<ProductDetailActivity> scenario = ActivityScenario.launch(ProductDetailActivity.intent(context, host))) {
            scenario.onActivity(activity -> {
                RecyclerView rv = new RecyclerView(activity);
                activity.setContentView(rv);
                VideoFeedAdapter adapter = new VideoFeedAdapter(Collections.singletonList(item));
                rv.setAdapter(adapter);
                recycler.set(rv); adapterRef.set(adapter);
            });
            AtomicReference<View> row = new AtomicReference<>();
            for (int i = 0; i < 50 && row.get() == null; i++) {
                scenario.onActivity(activity -> { RecyclerView.ViewHolder holder = recycler.get().findViewHolderForAdapterPosition(0); if (holder != null) row.set(holder.itemView); });
                Thread.sleep(100);
            }
            assertNotNull(row.get());
            scenario.onActivity(activity -> {
                VideoFeedAdapter adapter = adapterRef.get();
                adapter.onHostPause();
                reactions.setLiked(item.getId(), true);
                content.put("saved", NewsContract.videoSnapshot(item, ""));
                adapter.onHostResume();
                assertEquals("取消收藏", row.get().findViewById(R.id.iv_full_collect).getContentDescription());
                assertEquals("1", ((android.widget.TextView)row.get().findViewById(R.id.tv_full_like)).getText().toString());
                adapter.release(); recycler.get().setAdapter(null);
            });
        } finally {
            reactions.setLiked(item.getId(), false);
            content.remove("saved", NewsContract.videoNewsId(item.getTitle()));
        }
    }
}
