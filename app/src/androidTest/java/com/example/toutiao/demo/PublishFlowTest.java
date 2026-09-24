package com.example.toutiao.demo;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PublishFlowTest {

    private Context context;
    private ContentStore store;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        store = new ContentStore(context);
        store.clear("posts");
        store.clear("collections");
        store.clear("history");
    }

    @Test
    public void testPostPersistenceAndLibraryManagement() {
        // 1. Initial state should be empty
        JSONArray initialPosts = store.list("posts");
        assertEquals("Initially user posts should be empty", 0, initialPosts.length());

        // 2. Publish a post to ContentStore
        String title = "探索智能座舱与车载AI交互未来";
        String info = "本地发布 · 09-24 20:00 · 深圳市·南山区";
        String content = "今天在智能座舱展区深度体验了新一代多模态人机交互系统，手势控制与大模型语言理解非常丝滑。#科技数码前沿# 📍 深圳市·南山区";
        JSONObject postObj = ContentStore.article(title, info, content, R.drawable.news_smart_city, News.TYPE_SINGLE_IMG);
        store.put("posts", postObj);

        // 3. Verify persistence in ContentStore
        JSONArray savedPosts = store.list("posts");
        assertEquals(1, savedPosts.length());
        JSONObject first = savedPosts.optJSONObject(0);
        assertNotNull(first);
        assertEquals(title, first.optString("title"));
        assertEquals(info, first.optString("info"));
        assertEquals(content, first.optString("content"));
        assertEquals(News.TYPE_SINGLE_IMG, first.optInt("type"));
        assertTrue(store.contains("posts", title));

        // 4. Verify HomeFragment dynamic insertion
        News postNews = new News(News.TYPE_SINGLE_IMG, title, info, "刚刚", R.drawable.news_smart_city, 0, 0).withContent(content);
        HomeFragment.addUserPost(postNews);

        // 5. Publish a second post
        String title2 = "周末徒步：大自然中的治愈时光";
        String info2 = "本地发布 · 09-24 20:05 · 北京市·海淀区";
        String content2 = "远离城市喧嚣，感受山间微风与林海听涛。#大国工匠的日常# 📍 北京市·海淀区";
        JSONObject postObj2 = ContentStore.article(title2, info2, content2, R.drawable.news_nature_park, News.TYPE_SINGLE_IMG);
        store.put("posts", postObj2);

        JSONArray twoPosts = store.list("posts");
        assertEquals(2, twoPosts.length());
        // Most recent should be at index 0
        assertEquals(title2, twoPosts.optJSONObject(0).optString("title"));

        // 6. Delete the first post and verify deletion
        store.remove("posts", title);
        assertFalse(store.contains("posts", title));
        assertTrue(store.contains("posts", title2));
        assertEquals(1, store.list("posts").length());
    }

    @Test
    public void testFavoritesAndHistoryManagement() {
        // Add to collections
        String favTitle = "深中通道正式通车运营";
        JSONObject fav = ContentStore.article(favTitle, "新华社", "震撼！大国工程的世界级成就。", R.drawable.news_smart_city, News.TYPE_SINGLE_IMG);
        store.put("collections", fav);

        assertTrue(store.contains("collections", favTitle));
        assertEquals(1, store.list("collections").length());

        // Add to history
        String histTitle = "中国空间站生命科学实验突破";
        JSONObject hist = ContentStore.article(histTitle, "央视新闻", "太空育种成果初见端倪。", R.drawable.news_space_rocket, News.TYPE_THREE_IMG);
        store.put("history", hist);

        assertTrue(store.contains("history", histTitle));
        assertEquals(1, store.list("history").length());

        // Remove from collections
        store.remove("collections", favTitle);
        assertFalse(store.contains("collections", favTitle));
        assertEquals(0, store.list("collections").length());
    }
}
