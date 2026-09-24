package com.example.toutiao.demo;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OfflineVideoTest {
 @Test public void bundledSampleIsDecodableVideo() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  int id=c.getResources().getIdentifier("course_motion_1","raw",c.getPackageName());
  assertTrue("A real bundled video must exist",id!=0);
  try(android.content.res.AssetFileDescriptor fd=c.getResources().openRawResourceFd(id)){
   MediaMetadataRetriever r=new MediaMetadataRetriever();
   r.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
   assertTrue(Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))>=5000);
   assertNotNull(r.getFrameAtTime(1000000)); r.release();
  }
 }
 @Test public void interactionStateSurvivesNewStoreAndUsesStableIdentity() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  Class<?> type=Class.forName("com.example.toutiao.demo.VideoStore");
  Object store=type.getConstructor(Context.class).newInstance(c);
  type.getMethod("setLiked",String.class,boolean.class).invoke(store,"test-stable-id",true);
  Object reopened=type.getConstructor(Context.class).newInstance(c);
  assertEquals(true,type.getMethod("isLiked",String.class).invoke(reopened,"test-stable-id"));
  assertEquals(false,type.getMethod("isLiked",String.class).invoke(reopened,"different-test-id"));
  type.getMethod("setLiked",String.class,boolean.class).invoke(store,"test-stable-id",false);
 }
 @Test public void detailActuallyPlaysAndStopsWhenBackgrounded() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  android.content.Intent intent=new android.content.Intent(c,VideoDetailActivity.class).putExtra(VideoDetailActivity.EXTRA_TITLE,"播放器生命周期测试");
  try(androidx.test.core.app.ActivityScenario<VideoDetailActivity> scenario=androidx.test.core.app.ActivityScenario.launch(intent)) {
   java.util.concurrent.atomic.AtomicBoolean playing=new java.util.concurrent.atomic.AtomicBoolean();
   for(int i=0;i<40&&!playing.get();i++){
    Thread.sleep(150);
    scenario.onActivity(a->{
     OfflinePlayer p=a.findViewById(R.id.player_detail);
     if(p!=null) playing.set(p.isPlaying());
    });
   }
   assertTrue("Real media playback should start",playing.get());
   final OfflinePlayer[] player=new OfflinePlayer[1];
   scenario.onActivity(a->player[0]=a.findViewById(R.id.player_detail));
   scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
   InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
    if(player[0]!=null) assertFalse(player[0].isPlaying());
   });
  }
 }
}
