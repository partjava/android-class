package com.example.toutiao.demo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
/** Local audio with explicit loading, pause, seeking and lifecycle ownership. */
public class AudioDetailActivity extends AppCompatActivity {
 private MediaPlayer player; private boolean prepared; private int position;
 private Button play; private SeekBar seek; private TextView status;
 private SeekBar.OnSeekBarChangeListener seekListener;
 private android.content.res.AssetFileDescriptor afd;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){if(player!=null&&prepared){position=player.getCurrentPosition();seek.setProgress(position);status.setText(format(position)+" / "+format(player.getDuration()));handler.postDelayed(this,250);}}};
 @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_audio_detail);
  if(state!=null)position=state.getInt("position");
  ((TextView)findViewById(R.id.audio_title)).setText(getIntent().getStringExtra("title"));
  ((TextView)findViewById(R.id.audio_content)).setText(getIntent().getStringExtra("content"));
  status=findViewById(R.id.audio_status);play=findViewById(R.id.audio_play);seek=findViewById(R.id.audio_seek);
  findViewById(R.id.audio_back).setOnClickListener(v->finish());
  org.json.JSONObject row=NewsContract.snapshot(getIntent()); ContentStore store=new ContentStore(this);store.put("history",row);
  Button save=findViewById(R.id.audio_save);String id=row.optString("news_id");save.setText(store.contains("saved",id)?"取消收藏":"收藏音频");
  save.setOnClickListener(v->{if(store.contains("saved",id))store.remove("saved",id);else store.put("saved",row);save.setText(store.contains("saved",id)?"取消收藏":"收藏音频");});
  play.setOnClickListener(v->{if(player==null)open(true);else if(prepared){if(player.isPlaying()){player.pause();play.setText("播放");}else{player.start();play.setText("暂停");handler.removeCallbacks(tick);handler.post(tick);}}});
  seekListener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user&&prepared&&player!=null){position=p;player.seekTo(p);}} public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};
  seek.setOnSeekBarChangeListener(seekListener);
  open(false);
 }
 private void open(boolean autoplay){
  status.setText("正在加载音频…");play.setEnabled(false);
  try{String path=getIntent().getStringExtra("media_uri");if(path==null)path="news/campus_tone.wav";
   if(afd!=null){try{afd.close();}catch(Exception ignored){}afd=null;}
   afd=getAssets().openFd(path);
   player=new MediaPlayer();player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
   player.setOnPreparedListener(p->{if(player!=p)return;prepared=true;seek.setMax(p.getDuration());p.seekTo(position);play.setEnabled(true);play.setText(autoplay?"暂停":"播放");if(autoplay)p.start();handler.post(tick);});
   player.setOnCompletionListener(p->{position=0;play.setText("重播");});
   player.setOnErrorListener((p,w,e)->{release();status.setText("音频无法播放，点击播放重试");play.setEnabled(true);play.setText("重试");return true;});
   player.prepareAsync();
  }catch(Exception error){release();status.setText("音频加载失败，点击重试");play.setEnabled(true);play.setText("重试");}
 }
 private String format(int milliseconds){int seconds=milliseconds/1000;return String.format(java.util.Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
 private void release(){handler.removeCallbacks(tick);if(player!=null){if(prepared)try{position=player.getCurrentPosition();}catch(IllegalStateException ignored){}player.release();player=null;}if(afd!=null){try{afd.close();}catch(Exception ignored){}afd=null;}prepared=false;}
 @Override protected void onPause(){release();play.setText("播放");play.setEnabled(true);super.onPause();}
 @Override protected void onSaveInstanceState(Bundle state){state.putInt("position",position);super.onSaveInstanceState(state);}
 @Override protected void onDestroy(){release();super.onDestroy();}
}
