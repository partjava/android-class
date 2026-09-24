package com.example.toutiao.demo;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** 本地阅读记录、收藏和作品，以静态课程数据的标题去重。 */
public final class ContentStore {
    private final SharedPreferences prefs;
    public ContentStore(Context context) { prefs=context.getSharedPreferences("content",Context.MODE_PRIVATE); }
    public JSONArray list(String kind) {
        try { return new JSONArray(prefs.getString(kind,"[]")); } catch(Exception e) { return new JSONArray(); }
    }
    public boolean contains(String kind,String title) {
        JSONArray rows=list(kind);
        for(int i=0;i<rows.length();i++) if(rows.optJSONObject(i)!=null && title.equals(rows.optJSONObject(i).optString("title"))) return true;
        return false;
    }
    public void remove(String kind,String title) {
        JSONArray rows=list(kind), result=new JSONArray();
        for(int i=0;i<rows.length();i++) { JSONObject row=rows.optJSONObject(i); if(row!=null && !title.equals(row.optString("title"))) result.put(row); }
        prefs.edit().putString(kind,result.toString()).apply();
    }
    public void put(String kind,JSONObject item) {
        remove(kind,item.optString("title"));
        JSONArray old=list(kind), result=new JSONArray(); result.put(item);
        for(int i=0;i<old.length() && i<99;i++) result.put(old.optJSONObject(i));
        prefs.edit().putString(kind,result.toString()).apply();
    }
    public void clear(String kind) { prefs.edit().remove(kind).apply(); }
    public static JSONObject article(String title,String info,String content,int image,int type) {
        JSONObject result=new JSONObject();
        try { result.put("title",title); result.put("info",info); result.put("content",content); result.put("img",image); result.put("type",type); } catch(Exception ignored) { }
        return result;
    }
}
