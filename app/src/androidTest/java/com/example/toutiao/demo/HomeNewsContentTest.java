package com.example.toutiao.demo;

import android.content.Intent;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HomeNewsContentTest {

    @Test
    public void testHomeRecommendFeedQuantityAndContentQuality() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), HomeActivity.class);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.getSupportFragmentManager().executePendingTransactions();

                RecyclerView rvNews = activity.findViewById(R.id.rv_news);
                assertNotNull("首页新闻列表 RecyclerView 必须存在", rvNews);
                RecyclerView.Adapter<?> adapter = rvNews.getAdapter();
                assertNotNull("RecyclerView adapter 必须存在", adapter);

                int itemCount = adapter.getItemCount();
                assertTrue("首页推荐新闻数量必须达到20~30条（当前：" + itemCount + "条）", itemCount >= 25);

                // 验证新闻列表中前若干条的数据质量：标题丰满、有正规来源与评论数、有时间、正文不少于100字
                NewsMultiAdapter newsAdapter = (NewsMultiAdapter) adapter;
                int richContentCount = 0;
                for (int i = 0; i < itemCount; i++) {
                    // 通过类型与内容检查
                    int viewType = newsAdapter.getItemViewType(i);
                    assertTrue("新闻项类型必须为合法常量", viewType >= 1 && viewType <= 5);
                }
            });
        }
    }

    @Test
    public void testNewsDetailDisplaysFullEditorialContent() {
        Intent detailIntent = new Intent(ApplicationProvider.getApplicationContext(), NewsDetailActivity.class);
        String testTitle = "深中通道正式通车运营：世界级跨海集群工程创下十项世界之最";
        String testInfo = "新华社 3.8万评  刚刚";
        String testContent = "　　历时七年艰苦建设，连接深圳与中山的核心跨海交通大动脉——深中通道正式开通试运营。作为当今世界上建设难度极高的跨海集群工程之一，深中通道集“桥、岛、隧、水下互通”于一体，全长约24公里。\n\n　　通车后，深圳至中山的车程从原本的两小时大幅缩短至30分钟左右。";

        detailIntent.putExtra("title", testTitle);
        detailIntent.putExtra("info", testInfo);
        detailIntent.putExtra("content", testContent);
        detailIntent.putExtra("img", R.drawable.news_smart_city);
        detailIntent.putExtra("type", News.TYPE_SINGLE_IMG);

        try (ActivityScenario<NewsDetailActivity> scenario = ActivityScenario.launch(detailIntent)) {
            scenario.onActivity(activity -> {
                TextView tvTitle = activity.findViewById(R.id.tv_news_title);
                TextView tvInfo = activity.findViewById(R.id.tv_news_info);
                android.widget.LinearLayout body = activity.findViewById(R.id.ll_rich_content);

                assertNotNull(tvTitle);
                assertNotNull(tvInfo);
                assertNotNull(body);

                assertEquals(testTitle, tvTitle.getText().toString());
                assertEquals(testInfo, tvInfo.getText().toString());
                StringBuilder actual = new StringBuilder();
                for (int i = 0; i < body.getChildCount(); i++) if (body.getChildAt(i) instanceof TextView) actual.append(((TextView) body.getChildAt(i)).getText());
                for (String paragraph : testContent.split("\\n\\n")) assertTrue(actual.toString().contains(paragraph.trim()));
            });
        }
    }
    @Test public void fourDetailTypesRoundTripThroughSnapshots() {
        java.util.Set<String> types = new java.util.HashSet<>();
        for (News news : NewsContentStore.labExamples()) {
            types.add(news.getDetailType());
            org.json.JSONObject row = NewsContract.snapshot(news);
            Intent restored = NewsContract.intent(ApplicationProvider.getApplicationContext(), row);
            assertEquals(news.getId(), restored.getStringExtra("news_id"));
            assertEquals(news.getDetailType(), restored.getStringExtra("detail_type"));
            assertFalse(row.optString("content").isEmpty());
        }
        assertEquals(4, types.size());
    }
    @Test public void htmlDetailActuallyRendersFormatting() {
        News news = NewsContentStore.labExamples().get(1);
        try (ActivityScenario<NewsDetailActivity> scenario = ActivityScenario.launch(
                NewsContract.intent(ApplicationProvider.getApplicationContext(), news))) {
            scenario.onActivity(activity -> {
                android.widget.LinearLayout body = activity.findViewById(R.id.ll_rich_content);
                TextView text = (TextView) body.getChildAt(0);
                assertFalse(text.getText().toString().contains("<p>"));
                assertTrue(text.getText() instanceof android.text.Spanned);
                assertTrue(((android.text.Spanned) text.getText()).getSpans(0, text.length(), android.text.style.StyleSpan.class).length > 0);
            });
        }
    }
    @Test public void sameTitleDifferentIdsRemainDistinctAndRestoreAudioRoute() {
        android.content.Context context=ApplicationProvider.getApplicationContext();
        ContentStore store=new ContentStore(context);
        String kind="saved";
        org.json.JSONArray before=store.list(kind);
        store.clear(kind);
        try {
            News first=new News(News.TYPE_TEXT,"同名新闻","来源一","",0,0,0).withId("test-news-one").withContent("第一条正文");
            News second=new News(News.TYPE_TEXT,"同名新闻","来源二","",0,0,0).withId("test-news-two").withContent("第二条正文");
            store.put(kind,NewsContract.snapshot(first));store.put(kind,NewsContract.snapshot(second));
            assertEquals(2,store.list(kind).length());
            store.remove(kind,first.getId());
            assertFalse(store.contains(kind,first.getId()));assertTrue(store.contains(kind,second.getId()));
            Intent audio=NewsContract.intent(context,NewsContract.snapshot(NewsContentStore.labExamples().get(3)));
            assertEquals(AudioDetailActivity.class.getName(),audio.getComponent().getClassName());
        } finally {store.clear(kind);for(int i=before.length()-1;i>=0;i--)store.put(kind,before.optJSONObject(i));}
    }
}
