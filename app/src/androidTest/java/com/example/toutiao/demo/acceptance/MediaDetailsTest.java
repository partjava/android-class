package com.example.toutiao.demo.acceptance;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.webkit.WebView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.toutiao.demo.*;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

/** Only run in the isolated acceptance suite; every content mutation is restored. */
@RunWith(AndroidJUnit4.class)
public class MediaDetailsTest {
 private Context context;
 private ContentStore store;
 private JSONArray history, saved;
 @Before public void backup(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();store=new ContentStore(context);history=store.list("history");saved=store.list("saved");}
 @After public void restore(){restore("history",history);restore("saved",saved);}
 private void restore(String kind,JSONArray rows){if(rows==null)return;store.clear(kind);for(int i=rows.length()-1;i>=0;i--)store.put(kind,rows.optJSONObject(i));}
 private Object field(Object owner,String name){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(Exception e){throw new AssertionError(e);}}
 private void await(BooleanSupplier condition) throws Exception {long deadline=System.currentTimeMillis()+8000;while(!condition.getAsBoolean()&&System.currentTimeMillis()<deadline)Thread.sleep(100);assertTrue("Timed out waiting for media state",condition.getAsBoolean());}
 private <A extends Activity> boolean check(ActivityScenario<A> scenario,java.util.function.Predicate<A> check){final boolean[] result={false};scenario.onActivity(a->result[0]=check.test(a));return result[0];}
 private Activity resumed(){AtomicReference<Activity> result=new AtomicReference<>();InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))result.set(a);});return result.get();}
 private void screenshot(String name) throws Exception {
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
  File dir=new File(context.getExternalFilesDir(null),"lab-evidence");assertTrue(dir.isDirectory()||dir.mkdirs());
  try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
 }
 @Test public void remoteUrlKeepsIdentityWhenHeadlineChanges(){
  News before=new News(News.TYPE_TEXT,"原始标题","来源","",0,0,0).withRemote(null,"https://example.org/article/7");
  News after=new News(News.TYPE_BIG_IMG,"修订标题","不同来源","",0,0,0).withRemote(null,"https://example.org/article/7");
  assertEquals(before.getId(),after.getId());
 }
 @Test public void homepageActuallyOpensEachDetailKind() throws Exception {
  for(News example:NewsContentStore.labExamples()){
   try(ActivityScenario<HomeActivity> scenario=ActivityScenario.launch(HomeActivity.class)){
    final int[] position={-1};
    scenario.onActivity(a->{a.getSupportFragmentManager().executePendingTransactions();RecyclerView list=a.findViewById(R.id.rv_news);assertNotNull(list);List<?> news=(List<?>)field(list.getAdapter(),"newsList");for(int i=0;i<news.size();i++)if(example.getId().equals(((News)news.get(i)).getId()))position[0]=i;assertTrue(position[0]>=0);list.scrollToPosition(position[0]);});
    onView(withText(example.getTitle())).perform(click());
    Activity actual=resumed();assertNotNull(actual);
    Class<?> expected=NewsContract.AUDIO.equals(example.getDetailType())?AudioDetailActivity.class:NewsContract.VIDEO.equals(example.getDetailType())?VideoDetailActivity.class:NewsDetailActivity.class;
    assertEquals(expected,actual.getClass());assertEquals(example.getId(),actual.getIntent().getStringExtra("news_id"));
    pressBack();
   }
  }
 }
 @Test public void webLoadsRealLocalDocumentAndHtmlPreservesFormatting() throws Exception {
  try(ActivityScenario<NewsDetailActivity> scenario=ActivityScenario.launch(NewsContract.intent(context,NewsContentStore.labExamples().get(0)))){
   AtomicReference<String> dom=new AtomicReference<>("");
   scenario.onActivity(a->{
    WebView web=(WebView)field(a,"webView");
    assertFalse("Production local news must disable scripting",web.getSettings().getJavaScriptEnabled());
    assertTrue(web.getUrl().startsWith("file:///android_asset/news/"));
    // DOM inspection uses scripting only inside this test, on the bundled local document.
    web.getSettings().setJavaScriptEnabled(true);
    web.reload();
   });
   await(()->{scenario.onActivity(a->((WebView)field(a,"webView")).evaluateJavascript("document.body ? document.body.innerText : ''",value->{
    try { dom.set(new JSONArray("["+value+"]").optString(0,"")); } catch(Exception ignored) { dom.set(""); }
   }));return dom.get().contains("从事实开始")&&dom.get().contains("校园阅读指南");});
   scenario.onActivity(a->((WebView)field(a,"webView")).getSettings().setJavaScriptEnabled(false));
   screenshot("news-web.png");
  }
  try(ActivityScenario<NewsDetailActivity> scenario=ActivityScenario.launch(NewsContract.intent(context,NewsContentStore.labExamples().get(1)))){
   scenario.onActivity(a->{android.widget.LinearLayout body=a.findViewById(R.id.ll_rich_content);TextView text=(TextView)body.getChildAt(0);assertTrue(text.getText().toString().contains("标题与导语"));assertFalse(text.getText().toString().contains("<strong>"));android.text.Spanned spans=(android.text.Spanned)text.getText();assertTrue(spans.getSpans(0,spans.length(),android.text.style.StyleSpan.class).length>0);});
   screenshot("news-html.png");
  }
 }
 @Test public void ordinaryNativeParagraphsAreNotParsedAsHtml() {
  News plain=new News(News.TYPE_TEXT,"原生段落验收","验收","",0,0,0).withId("acceptance-native-paragraphs").withContent("第一段含比较符号：1 < 2。\n\n第二段保留原始文本与分段。");
  try(ActivityScenario<NewsDetailActivity> scenario=ActivityScenario.launch(NewsContract.intent(context,plain))){
   scenario.onActivity(a->{android.widget.LinearLayout body=a.findViewById(R.id.ll_rich_content);assertTrue(body.getChildCount()>=3);assertTrue(((TextView)body.getChildAt(0)).getText().toString().contains("1 < 2"));assertTrue(((TextView)body.getChildAt(1)).getText().toString().contains("第二段"));});
  }
 }
 @Test public void audioAdvancesSeeksPausesAndReleasesOnBackgroundAndDestroy() throws Exception {
  try(ActivityScenario<AudioDetailActivity> scenario=ActivityScenario.launch(NewsContract.intent(context,NewsContentStore.labExamples().get(3)))){
   await(()->check(scenario,a->Boolean.TRUE.equals(field(a,"prepared"))));
   onView(withText("播放")).perform(click());
   await(()->check(scenario,a->((MediaPlayer)field(a,"player")).isPlaying()&&((SeekBar)a.findViewById(R.id.audio_seek)).getProgress()>300));
   scenario.onActivity(a->{SeekBar seek=a.findViewById(R.id.audio_seek);Object listener=field(a,"seekListener");((SeekBar.OnSeekBarChangeListener)listener).onProgressChanged(seek,6000,true);});
   await(()->check(scenario,a->((MediaPlayer)field(a,"player")).getCurrentPosition()>=5900));
   onView(withText("暂停")).perform(click());
   int[] paused={0};scenario.onActivity(a->{MediaPlayer p=(MediaPlayer)field(a,"player");assertFalse(p.isPlaying());paused[0]=p.getCurrentPosition();});Thread.sleep(350);
   scenario.onActivity(a->assertTrue(Math.abs(((MediaPlayer)field(a,"player")).getCurrentPosition()-paused[0])<150));
   screenshot("news-audio.png");
   AtomicReference<AudioDetailActivity> reference=new AtomicReference<>();scenario.onActivity(reference::set);
   scenario.moveToState(Lifecycle.State.CREATED);InstrumentationRegistry.getInstrumentation().runOnMainSync(()->assertNull(field(reference.get(),"player")));
   scenario.moveToState(Lifecycle.State.RESUMED);InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->a.findViewById(R.id.audio_play).performClick());
   await(()->check(scenario,a->Boolean.TRUE.equals(field(a,"prepared"))));
   scenario.moveToState(Lifecycle.State.DESTROYED);InstrumentationRegistry.getInstrumentation().runOnMainSync(()->assertNull(field(reference.get(),"player")));
  }
 }
 @Test public void audioBadUriShowsRetryAndCanRecover() throws Exception {
  Intent intent=NewsContract.intent(context,NewsContentStore.labExamples().get(3)).putExtra("media_uri","news/missing.wav");
  try(ActivityScenario<AudioDetailActivity> scenario=ActivityScenario.launch(intent)){
   scenario.onActivity(a->{assertNull(field(a,"player"));assertTrue(((TextView)a.findViewById(R.id.audio_status)).getText().toString().contains("失败"));a.getIntent().putExtra("media_uri","news/campus_tone.wav");});
   onView(withText("重试")).perform(click());await(()->check(scenario,a->Boolean.TRUE.equals(field(a,"prepared"))&&((MediaPlayer)field(a,"player")).isPlaying()));
  }
 }
 @Test public void videoProgressComesFromPlayerAndReleasesInBackground() throws Exception {
  try(ActivityScenario<VideoDetailActivity> scenario=ActivityScenario.launch(NewsContract.intent(context,NewsContentStore.labExamples().get(2)))){
   await(()->check(scenario,a->{OfflinePlayer p=a.findViewById(R.id.player_detail);return p.isPlaying()&&p.progress()>4;}));
   await(()->check(scenario,a->{OfflinePlayer p=a.findViewById(R.id.player_detail);int rendered=(Integer)field(a,"progress");return Math.abs(rendered-p.progress())<=5&&a.findViewById(R.id.v_detail_progress).getLayoutParams().width>0;}));
   screenshot("news-video.png");AtomicReference<OfflinePlayer> reference=new AtomicReference<>();scenario.onActivity(a->reference.set(a.findViewById(R.id.player_detail)));
   scenario.moveToState(Lifecycle.State.CREATED);InstrumentationRegistry.getInstrumentation().runOnMainSync(()->assertFalse(reference.get().isPlaying()));
  }
 }
}
