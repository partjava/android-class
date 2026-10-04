package com.example.toutiao.demo;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * 天行数据「头条新闻」接口客户端（https://www.tianapi.com/apiview/13）。
 *
 * 用法：到 tianapi.com 注册（免费）→ 申请「头条新闻」接口 → 把 key 填到
 * 下面的 API_KEY，首页启动后推荐频道顶部就会出现接口拉回的实时头条。
 * key 保持占位值时不发起任何请求，App 与原来一样纯本地运行。
 *
 * 数据照旧走本项目的两段式架构：接口返回的列表摘要（标题/来源/时间/封面）
 * 进首页信息流；description 摘要注册进 NewsContentStore 运行时层，
 * 详情页进来才按标题查询——接口文档的 description 只是摘要而非全文，
 * 全文在 url 字段指向的原网页，详情页提供「阅读原文」跳浏览器。
 */
public final class NewsApiStore {

    /** 天行数据控制台把你的 key 粘到这里；保持占位值 = 不启用联网加载 */
    public static final String API_KEY = "请在这里填入你的TianAPI密钥";

    private static final String ENDPOINT = "https://api.tianapi.com/toutiao/index";
    private static final int TIMEOUT_MS = 8000;

    /** 本次进程已拉到的头条，HomeFragment 组装推荐频道时拼在本地文章前面 */
    private static final List<News> CACHE = new ArrayList<>();
    private static volatile boolean fetched = false;

    /** 回调保证在主线程执行 */
    public interface Callback {
        void onSuccess(List<News> items);
        void onFailure(String reason);
    }

    private NewsApiStore() { }

    /** key 还是占位符时视为未配置，不联网 */
    public static boolean isConfigured() {
        return API_KEY != null && !API_KEY.isEmpty() && !API_KEY.contains("填入");
    }

    /** 已拉到的头条（可能为空）；只读，UI 线程直接拼列表用 */
    public static List<News> cachedHeadlines() {
        return CACHE;
    }

    /** 拉取成功后由主线程回调里调用：进缓存，之后 buildRecommend 就能拼上 */
    public static void cache(List<News> items) {
        CACHE.clear();
        CACHE.addAll(items);
    }

    /** 每个进程只拉一次；重复调用直接忽略，避免刷 onResume 反复请求 */
    public static void fetchHeadlines(final int num, final Callback cb) {
        if (!isConfigured() || fetched) {
            return;
        }
        fetched = true;
        new Thread(() -> {
            try {
                URL url = new URL(ENDPOINT
                        + "?key=" + URLEncoder.encode(API_KEY, "UTF-8")
                        + "&num=" + num);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                if (conn.getResponseCode() != 200) {
                    throw new IOException("HTTP " + conn.getResponseCode());
                }
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                List<News> items = parse(sb.toString());
                post(() -> {
                    if (items.isEmpty()) {
                        cb.onFailure("接口返回空数据");
                    } else {
                        cb.onSuccess(items);
                    }
                });
            } catch (Exception e) {
                fetched = false; //失败允许下次再试
                post(() -> cb.onFailure(e.getMessage() == null ? "网络异常" : e.getMessage()));
            }
        }).start();
    }

    private static void post(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    /**
     * 解析 newslist：同一批按标题去重，也跳过与本地文章库同名的，
     * 防止信息流里出现两条一样的文章
     */
    private static List<News> parse(String json) throws JSONException {
        List<News> items = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        if (root.optInt("code", 0) != 200) {
            return items;
        }
        JSONArray list = root.optJSONArray("newslist");
        if (list == null) {
            return items;
        }
        for (int i = 0; i < list.length(); i++) {
            JSONObject o = list.getJSONObject(i);
            String title = o.optString("title", "").trim();
            if (title.isEmpty() || NewsContentStore.contentOf(title) != null) {
                continue;
            }
            boolean dup = false;
            for (News n : items) {
                if (n.getTitle().equals(title)) {
                    dup = true;
                    break;
                }
            }
            if (dup) {
                continue;
            }

            String description = o.optString("description", "").trim();
            String picUrl = o.optString("picUrl", "").trim();
            String linkUrl = o.optString("url", "").trim();
            String ctime = o.optString("ctime", "").trim();
            String source = o.optString("source", o.optString("src", "实时资讯")).trim();

            News news = picUrl.isEmpty()
                    ? new News(News.TYPE_TEXT, title, source, ctime, 0, 0, 0)
                    : new News(News.TYPE_SINGLE_IMG, title, source, ctime, 0, 0, 0);
            news.withRemote(picUrl.isEmpty() ? null : picUrl, linkUrl.isEmpty() ? null : linkUrl);
            NewsContentStore.registerRuntime(title, description);
            items.add(news);
        }
        return items;
    }
}
