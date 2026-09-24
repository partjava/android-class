package com.example.toutiao.demo;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
/** Stable IDs keep local reactions independent of list order and process lifetime. */
public class VideoStore {
 private final SharedPreferences prefs;
 public VideoStore(Context c){prefs=c.getApplicationContext().getSharedPreferences("video_interactions_v1",Context.MODE_PRIVATE);}
 public boolean isFollowing(){return prefs.getBoolean("follow:offline-classroom",false);}
 public void setFollowing(boolean value){prefs.edit().putBoolean("follow:offline-classroom",value).apply();}
 public boolean isLiked(String id){return prefs.getBoolean("like:"+id,false);}
 public void setLiked(String id,boolean value){prefs.edit().putBoolean("like:"+id,value).apply();}
 public boolean isCollected(String id){return prefs.getBoolean("collect:"+id,false);}
 public void setCollected(String id,boolean value){prefs.edit().putBoolean("collect:"+id,value).apply();}
 public List<String> comments(String id){List<String> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("comments:"+id,"[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}return out;}
 public void addComment(String id,String text){String clean=text.trim();if(clean.isEmpty())return;List<String> list=comments(id);list.add(clean);prefs.edit().putString("comments:"+id,new JSONArray(list).toString()).apply();}
}
