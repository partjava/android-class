package com.example.toutiao.demo;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class CommentStore {
    private static final String PREF_NAME = "news_interaction_pref";
    private final SharedPreferences sp;
    private final Context context;

    public static class Comment {
        public String id;
        public String author;
        public String tag;
        public String time;
        public String content;
        public int likeCount;
        public boolean isLiked;

        public Comment(String id, String author, String tag, String time, String content, int likeCount, boolean isLiked) {
            this.id = id;
            this.author = author;
            this.tag = tag;
            this.time = time;
            this.content = content;
            this.likeCount = likeCount;
            this.isLiked = isLiked;
        }
    }

    public CommentStore(Context context) {
        this.context = context.getApplicationContext();
        this.sp = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String getKey(String newsTitle) {
        return "comments_" + (newsTitle != null ? newsTitle.hashCode() : 0);
    }

    public List<Comment> getComments(String newsTitle) {
        String key = getKey(newsTitle);
        String raw = sp.getString(key, null);
        List<Comment> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            list = getDefaultComments();
            saveComments(newsTitle, list);
            return list;
        }

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new Comment(
                        obj.optString("id", String.valueOf(System.currentTimeMillis())),
                        obj.optString("author", "晴川校友"),
                        obj.optString("tag", "晴川学子"),
                        obj.optString("time", "刚刚"),
                        obj.optString("content", ""),
                        obj.optInt("likeCount", 0),
                        obj.optBoolean("isLiked", false)
                ));
            }
        } catch (Exception e) {
            list = getDefaultComments();
        }
        return list;
    }

    public void addComment(String newsTitle, String content) {
        List<Comment> list = getComments(newsTitle);
        String currentUserName = new ProfileStore(context).get("nickname");
        if (currentUserName == null || currentUserName.trim().isEmpty()) {
            currentUserName = "晴川追光者";
        }
        String id = "cmt_" + System.currentTimeMillis();
        Comment newComment = new Comment(
                id,
                currentUserName,
                "晴川认证用户",
                "刚刚",
                content,
                0,
                false
        );
        list.add(0, newComment);
        saveComments(newsTitle, list);
    }

    public boolean toggleCommentLike(String newsTitle, String commentId) {
        List<Comment> list = getComments(newsTitle);
        boolean nowLiked = false;
        for (Comment c : list) {
            if (c.id.equals(commentId)) {
                c.isLiked = !c.isLiked;
                c.likeCount += (c.isLiked ? 1 : -1);
                if (c.likeCount < 0) c.likeCount = 0;
                nowLiked = c.isLiked;
                break;
            }
        }
        saveComments(newsTitle, list);
        return nowLiked;
    }

    private void saveComments(String newsTitle, List<Comment> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Comment c : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", c.id);
                obj.put("author", c.author);
                obj.put("tag", c.tag);
                obj.put("time", c.time);
                obj.put("content", c.content);
                obj.put("likeCount", c.likeCount);
                obj.put("isLiked", c.isLiked);
                arr.put(obj);
            }
            sp.edit().putString(getKey(newsTitle), arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private List<Comment> getDefaultComments() {
        List<Comment> list = new ArrayList<>();
        list.add(new Comment("1", "晴川学子 · 计科学院", "晴川学子", "15分钟前", "为学校的高质量发展点赞！在龙泉山下看母校越来越好，倍感自豪！👍", 42, false));
        list.add(new Comment("2", "晨读打卡人 · 外语学院", "考研打卡", "1小时前", "晴川图书馆和汤逊湖畔真的是绝佳的读书自习圣地，加油晴川人！🌸", 28, false));
        list.add(new Comment("3", "大一萌新 · 机械电气", "校园新星", "3小时前", "表白大美晴川！新学期一起努力，向优秀榜样看齐～✨", 15, false));
        return list;
    }

    // ================= 文章点赞 =================
    private String getArticleLikeKey(String newsTitle) {
        return "article_like_" + (newsTitle != null ? newsTitle.hashCode() : 0);
    }

    private String getArticleCountKey(String newsTitle) {
        return "article_count_" + (newsTitle != null ? newsTitle.hashCode() : 0);
    }

    public boolean isArticleLiked(String newsTitle) {
        return sp.getBoolean(getArticleLikeKey(newsTitle), false);
    }

    public int getArticleLikes(String newsTitle) {
        int base = Math.abs((newsTitle != null ? newsTitle.hashCode() : 100) % 50) + 38;
        return sp.getInt(getArticleCountKey(newsTitle), base);
    }

    public int toggleArticleLike(String newsTitle) {
        boolean liked = isArticleLiked(newsTitle);
        int current = getArticleLikes(newsTitle);
        boolean nowLiked = !liked;
        int nextCount = current + (nowLiked ? 1 : -1);
        if (nextCount < 0) nextCount = 0;

        sp.edit()
                .putBoolean(getArticleLikeKey(newsTitle), nowLiked)
                .putInt(getArticleCountKey(newsTitle), nextCount)
                .apply();
        return nextCount;
    }
}
