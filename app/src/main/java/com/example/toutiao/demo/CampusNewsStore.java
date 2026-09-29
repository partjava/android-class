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
import java.util.ArrayList;
import java.util.List;

/**
 * 校园新闻 Flask 本地后端客户端。
 *
 * 每次用户在首页点击【校园】频道或点击【立即查询】按钮时，
 * 都会向本地运行的 Python Flask 服务发起一次 HTTP GET 请求：
 * · Android 模拟器访问地址：http://10.0.2.2:5000/api/news
 * · 真机通过局域网访问时可通过 setServerUrl(...) 切换 IP。
 *
 * 请求成功后动态更新校园新闻列表，并将全文注册进 NewsContentStore，
 * 点击即可进入详情页查看完整校园资讯。
 */
public final class CampusNewsStore {

    /** 模拟器默认访问宿主机 Flask 服务的地址 (10.0.2.2 映射到电脑 localhost) */
    private static String serverBaseUrl = "http://10.0.2.2:5000";
    private static final int TIMEOUT_MS = 5000;

    /** 最近一次从 Flask 查询拉取成功的校园新闻缓存 */
    private static final List<News> CACHED_CAMPUS_NEWS = new ArrayList<>();
    private static String lastQueryTime = "";
    private static String currentSchool = "校园网";

    public interface Callback {
        void onSuccess(List<News> items, String queryTime, String school);
        void onFailure(String reason);
    }

    public interface PageCallback {
        void onSuccess(List<News> items, String queryTime, String school, int currentPage, int pageCount, int newsCount, int perPage, boolean hasMore);
        void onFailure(String reason);
    }

    private CampusNewsStore() { }

    public static void setServerUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            serverBaseUrl = url.trim();
        }
    }

    public static String getServerUrl() {
        return serverBaseUrl;
    }

    public static List<News> getCachedNews() {
        return new ArrayList<>(CACHED_CAMPUS_NEWS);
    }

    public static String getLastQueryTime() {
        return lastQueryTime;
    }

    public static String getCurrentSchool() {
        return currentSchool;
    }

    /**
     * 兼容方法：默认拉取第一页
     */
    public static void fetchCampusNews(final Callback callback) {
        fetchCampusNews(1, 15, new PageCallback() {
            @Override
            public void onSuccess(List<News> items, String queryTime, String school, int currentPage, int pageCount, int newsCount, int perPage, boolean hasMore) {
                if (callback != null) {
                    callback.onSuccess(items, queryTime, school);
                }
            }

            @Override
            public void onFailure(String reason) {
                if (callback != null) {
                    callback.onFailure(reason);
                }
            }
        });
    }

    /**
     * 分页向后读取核心方法：实现“触底手势无线刷新，自动追加接上去”
     * 符合教师规范：/xiaoyuan/page/<page>，返回 newscount, pagecount, currentpage, perpage
     * @param page 页码（从 1 开始累加）
     * @param size 每批读取数量（默认 15 条）
     * @param callback 成功/失败回调
     */
    public static void fetchCampusNews(final int page, final int size, final PageCallback callback) {
        fetchCategoryNews("xxyw", page, size, callback);
    }

    /**
     * 分类实时爬取核心方法：支持学校要闻(xxyw)、通知公告(tzgg)、教育教学(jyjx)、媒体关注(mtgz)、学校荣誉(xxry)
     * @param category 栏目 ID 或拼音标识
     * @param page 页码（从 1 开始累加）
     * @param size 每批读取数量（默认 15 条）
     * @param callback 成功/失败回调
     */
    public static void fetchCategoryNews(final String category, final int page, final int size, final PageCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String cat = (category == null || category.trim().isEmpty()) ? "xxyw" : category.trim();
                String base = serverBaseUrl;
                if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
                String reqUrl = base + "/xiaoyuan/" + cat + "/page/" + page;

                URL url = new URL(reqUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                if (code != 200) {
                    throw new IOException("HTTP 状态码 " + code);
                }

                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                JSONObject root = new JSONObject(sb.toString());
                int resCode = root.optInt("code", 0);
                if (resCode != 200) {
                    throw new IOException(root.optString("msg", "服务端处理异常"));
                }

                String queryTime = root.optString("query_time", "刚刚");
                String school = root.optString("school", "武汉晴川学院");
                // 老师指定的标准接口参数解析:
                int currentPage = root.optInt("currentpage", root.optInt("page", page));
                int pageCount = root.optInt("pagecount", root.optInt("total_pages", 1));
                int newsCount = root.optInt("newscount", pageCount * 15);
                int perPage = root.optInt("perpage", size);
                boolean hasMore = root.optBoolean("has_more", currentPage < pageCount);

                JSONArray array = root.optJSONArray("newslist");
                if (array == null) {
                    array = root.optJSONArray("data");
                }

                List<News> parsedList = parseArray(array);

                if (currentPage <= 1) {
                    CACHED_CAMPUS_NEWS.clear();
                }
                CACHED_CAMPUS_NEWS.addAll(parsedList);
                lastQueryTime = queryTime;
                currentSchool = school;

                post(() -> {
                    if (callback != null) {
                        callback.onSuccess(parsedList, queryTime, school, currentPage, pageCount, newsCount, perPage, hasMore);
                    }
                });

            } catch (Exception e) {
                final String err = (e.getMessage() == null || e.getMessage().isEmpty())
                        ? "连接官网爬虫服务超时或异常" : e.getMessage();
                post(() -> {
                    if (callback != null) {
                        callback.onFailure(err);
                    }
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {}
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private static int getFallbackResId(String imgUrl, String title) {
        return 0;
    }

    private static int getSecondResId(String title) {
        return 0;
    }

    private static List<News> parseArray(JSONArray array) throws JSONException {
        List<News> list = new ArrayList<>();
        if (array == null) {
            return list;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String title = obj.optString("title", "").trim();
            if (title.isEmpty()) {
                continue;
            }

            String source = obj.optString("source", "党委宣传部 官方发布").trim();
            String time = obj.optString("time", "刚刚").trim();
            String content = obj.optString("content", "").trim();
            String link = obj.optString("link", "").trim();

            JSONArray imgs = obj.optJSONArray("images");
            int imgCount = imgs != null ? imgs.length() : 0;
            String imgUrl = "";
            String imgUrl2 = "";
            String imgUrl3 = "";
            if (imgCount > 0) imgUrl = imgs.optString(0, "");
            if (imgCount > 1) imgUrl2 = imgs.optString(1, "");
            if (imgCount > 2) imgUrl3 = imgs.optString(2, "");

            if (imgUrl.isEmpty()) imgUrl = obj.optString("img_url", "").trim();
            if (imgUrl2.isEmpty()) imgUrl2 = obj.optString("img_url_2", "").trim();

            // 多样化新闻卡片布局规则（告别单一右侧单图）：
            // 1. 无图文章 -> TYPE_TEXT (纯文字模式)
            // 2. 页面首篇头条 (i == 0) 或关键大图特稿 (如 i % 6 == 4) -> TYPE_BIG_IMG (通栏大图模式)
            // 3. 拥有 3 张及以上配图的多图现场报道 -> TYPE_THREE_IMG (经典三图横排模式)
            // 4. 普通 1~2 张配图新闻 -> TYPE_SINGLE_IMG (左文右图单图模式)
            int itemType;
            if (imgCount == 0 && imgUrl.isEmpty()) {
                itemType = News.TYPE_TEXT;
            } else if (i == 0 || (i % 6 == 4 && imgCount >= 1)) {
                itemType = News.TYPE_BIG_IMG;
            } else if (imgCount >= 3 && (i % 3 == 1 || i % 2 == 1)) {
                itemType = News.TYPE_THREE_IMG;
            } else {
                itemType = News.TYPE_SINGLE_IMG;
            }

            int fallbackRes = getFallbackResId(imgUrl, title);
            int secondRes = getSecondResId(title);
            News news = new News(itemType, title, source, time, fallbackRes, secondRes, 0);
            if (!content.isEmpty()) {
                news.withContent(content);
                NewsContentStore.registerRuntime(title, content);
            }
            if (!imgUrl.isEmpty()) {
                news.withRemote(imgUrl, link);
            }
            if (!imgUrl2.isEmpty()) {
                news.withRemote2(imgUrl2);
            }
            if (!imgUrl3.isEmpty()) {
                news.withRemote3(imgUrl3);
            }

            JSONArray blocks = obj.optJSONArray("blocks");
            if (blocks != null && blocks.length() > 0) {
                news.withBlocks(blocks.toString());
            }

            list.add(news);
        }
        return list;
    }

    private static void post(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
