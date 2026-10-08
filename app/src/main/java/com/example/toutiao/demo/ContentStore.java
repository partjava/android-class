package com.example.toutiao.demo;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** SQLite owns reading/saved records; existing posts and likes retain their preferences. */
public final class ContentStore {
    private final SharedPreferences prefs;
    private final Context context;
    private final DemoDatabase database;
    public ContentStore(Context context) { this.context = context.getApplicationContext(); prefs=this.context.getSharedPreferences("content",Context.MODE_PRIVATE); database=new DemoDatabase(this.context); }
    private boolean sqlite(String kind) { return "history".equals(kind) || "saved".equals(kind); }
    public JSONArray list(String kind) {
        if(sqlite(kind)) return database.list(kind);
        try { return new JSONArray(prefs.getString(kind,"[]")); } catch(Exception e) { return new JSONArray(); }
    }
    /** Resolve old likes from saved/history snapshots, with CommentStore as truth. */
    public JSONArray likedArticles() {
        java.util.List<JSONObject> snapshots = new java.util.ArrayList<>();
        CommentStore comments = new CommentStore(context);
        for (String kind : new String[]{"liked", "saved", "history"}) {
            JSONArray rows = list(kind);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                String title = row.optString("title");
                snapshots.add(row);
            }
        }
        JSONArray result = new JSONArray();
        for (JSONObject row : LikedArticleSelection.select(snapshots, item -> item.optString("title"), comments::isArticleLiked)) result.put(row);
        return result;
    }
    public boolean contains(String kind,String title) {
        JSONArray rows=list(kind);
        for(int i=0;i<rows.length();i++) if(matches(rows.optJSONObject(i),title)) return true;
        return false;
    }
    private boolean matches(JSONObject row,String key) {
        return row!=null && (key.equals(row.optString("news_id")) || key.equals(row.optString("title")));
    }
    public void remove(String kind,String title) {
        if(sqlite(kind)) {
            database.transaction(()->{JSONArray rows=database.list(kind);for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(matches(row,title))database.remove(kind,row.optString("news_id"));}return null;});
            return;
        }
        JSONArray rows=list(kind), result=new JSONArray();
        for(int i=0;i<rows.length();i++) { JSONObject row=rows.optJSONObject(i); if(row!=null && !matches(row,title)) result.put(row); }
        prefs.edit().putString(kind,result.toString()).apply();
    }
    public void put(String kind,JSONObject item) {
        if(sqlite(kind)) {
            database.transaction(()->{
                if(item.optString("news_id").isEmpty())item.put("news_id",LegacyIds.news(item.optString("title")));
                JSONArray rows=database.list(kind);
                // Adopt only old title-keyed snapshots; distinct modern IDs with equal titles remain distinct.
                for(int i=0;i<rows.length();i++) {JSONObject old=rows.getJSONObject(i);if(old.optString("news_id").startsWith("legacy-news-") && old.optString("title").equals(item.optString("title")))database.remove(kind,old.optString("news_id"));}
                if("history".equals(kind) && !item.optString("news_id").startsWith("legacy-news-")) {
                    JSONArray favorites=database.list("saved");
                    for(int i=0;i<favorites.length();i++) {
                        JSONObject old=favorites.getJSONObject(i);
                        if(old.optString("news_id").startsWith("legacy-news-") && old.optString("title").equals(item.optString("title"))) {
                            database.remove("saved",old.optString("news_id"));old.put("news_id",item.optString("news_id"));database.put("saved",old);
                        }
                    }
                }
                database.put(kind,item); return null;
            });
            return;
        }
        remove(kind,item.optString("news_id",item.optString("title")));
        JSONArray old=list(kind), result=new JSONArray(); result.put(item);
        for(int i=0;i<old.length() && i<99;i++) result.put(old.optJSONObject(i));
        prefs.edit().putString(kind,result.toString()).apply();
    }
    public void clear(String kind) { if(sqlite(kind))database.replace(kind,new JSONArray());else prefs.edit().remove(kind).apply(); }
    public static JSONObject article(String title,String info,String content,int image,int type) {
        return article(title, info, content, image, type, null, null, null, null, null);
    }
    public static JSONObject article(String title, String info, String content, int image, int type,
                                     String imgUrl, String imgUrl2, String imgUrl3, String blocksJson, String link) {
        JSONObject result = new JSONObject();
        try {
            result.put("title", title);
            result.put("news_id", LegacyIds.news(title));
            result.put("info", info);
            result.put("content", content);
            result.put("img", image);
            result.put("type", type);
            if (imgUrl != null && !imgUrl.isEmpty()) result.put("img_url", imgUrl);
            if (imgUrl2 != null && !imgUrl2.isEmpty()) result.put("img_url_2", imgUrl2);
            if (imgUrl3 != null && !imgUrl3.isEmpty()) result.put("img_url_3", imgUrl3);
            if (blocksJson != null && !blocksJson.isEmpty()) result.put("blocks_json", blocksJson);
            if (link != null && !link.isEmpty()) result.put("link", link);
        } catch (Exception ignored) { }
        return result;
    }
}
