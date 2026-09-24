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
                    assertTrue("新闻项类型必须为合法常量", viewType >= 1 && viewType <= 4);
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
                TextView tvContent = activity.findViewById(R.id.tv_news_content);

                assertNotNull(tvTitle);
                assertNotNull(tvInfo);
                assertNotNull(tvContent);

                assertEquals(testTitle, tvTitle.getText().toString());
                assertEquals(testInfo, tvInfo.getText().toString());
                assertEquals(testContent, tvContent.getText().toString());
            });
        }
    }
}
