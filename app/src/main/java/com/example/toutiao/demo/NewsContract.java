package com.example.toutiao.demo;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;
/** The same complete article snapshot is used for feed, history and saved items. */
public final class NewsContract {
 public static final String WEB="web", HTML="html", VIDEO="video", AUDIO="audio", ALBUM="album";
 private NewsContract() {}
 /** 视频快照稳定 id：与标题绑定，feed/详情/收藏/历史共用同一把钥匙。 */
 public static String videoNewsId(String displayTitle){return "video-"+VideoItem.stableId(displayTitle);}
 /** 视频快照：与文章共存于 history/saved 表，detail_type 区分。 */
 public static JSONObject videoSnapshot(VideoItem item,String albumId) {
  JSONObject row=ContentStore.article(item.getTitle(),"离线影像课堂",item.getDesc(),item.getCoverRes(),News.TYPE_VIDEO);
  try {
   row.put("news_id",videoNewsId(item.getTitle()));
   row.put("detail_type",VIDEO);
   row.put("source",item.getSource());
   row.put("desc",item.getDesc());
   row.put("cover",item.getCoverRes());
   row.put("duration",item.getDuration());
   row.put("play_count",item.getPlayCount());
   row.put("album_id",albumId==null?"":albumId);
  } catch(Exception ignored) {}
  return row;
 }
 /** 专辑快照：整体收藏/浏览记录共用。 */
 public static JSONObject albumSnapshot(VideoAlbum album) {
  JSONObject row=ContentStore.article(album.getTitle(),album.getSubtitle(),album.getSubtitle(),album.getCoverRes(),0);
  try {
   row.put("news_id","album-"+album.getId());
   row.put("detail_type",ALBUM);
   row.put("album_id",album.getId());
   row.put("album_count",album.getCount());
   row.put("update_info",album.getUpdateInfo());
   row.put("source",album.getSubtitle());
   row.put("cover",album.getCoverRes());
  } catch(Exception ignored) {}
  return row;
 }
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
  Class<?> target=AUDIO.equals(detail)?AudioDetailActivity.class
          :VIDEO.equals(detail)?VideoDetailActivity.class
          :ALBUM.equals(detail)?AlbumActivity.class
          :NewsDetailActivity.class;
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
