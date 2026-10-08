package com.example.toutiao.demo;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;
/** The same complete article snapshot is used for feed, history and saved items. */
public final class NewsContract {
 public static final String WEB="web", HTML="html", VIDEO="video", AUDIO="audio";
 private NewsContract() {}
 public static JSONObject snapshot(News news) {
  String body=news.getContent();
  if(body==null || body.trim().isEmpty()) body=NewsContentStore.contentOf(news.getId(),news.getTitle());
  JSONObject row=ContentStore.article(news.getTitle(),news.getSource()+"  "+news.getTime(),body,news.getImg1(),news.getType(),news.getImageUrl(),news.getImageUrl2(),news.getImageUrl3(),news.getBlocksJson(),news.getLinkUrl());
  try { row.put("news_id",news.getId()); row.put("detail_type",news.getDetailType()); row.put("media_uri",news.getMediaUri()); row.put("img2",news.getImg2()); row.put("img3",news.getImg3()); } catch(Exception ignored) {}
  return row;
 }
 public static Intent intent(Context context,News news){return intent(context,snapshot(news));}
 public static Intent intent(Context context,JSONObject row){
  String detail=row.optString("detail_type",row.optInt("type")==News.TYPE_VIDEO?VIDEO:HTML);
  Class<?> target=AUDIO.equals(detail)?AudioDetailActivity.class:VIDEO.equals(detail)?VideoDetailActivity.class:NewsDetailActivity.class;
  Intent intent=new Intent(context,target);
  java.util.Iterator<String> keys=row.keys();
  while(keys.hasNext()){String key=keys.next(); Object value=row.opt(key);if(value instanceof Number)intent.putExtra(key,((Number)value).intValue());else if(value instanceof String)intent.putExtra(key,(String)value);}
  intent.putExtra("detail_type",detail);
  intent.putExtra("source",row.optString("info"));intent.putExtra("desc",row.optString("content"));intent.putExtra("cover",row.optInt("img"));
  return intent;
 }
 public static JSONObject snapshot(Intent intent){
  JSONObject row=new JSONObject(); android.os.Bundle extras=intent.getExtras();
  if(extras!=null)for(String key:extras.keySet())try{row.put(key,extras.get(key));}catch(Exception ignored){}
  if(!row.has("news_id"))try{row.put("news_id",LegacyIds.news(row.optString("title")));}catch(Exception ignored){}
  return row;
 }
}
