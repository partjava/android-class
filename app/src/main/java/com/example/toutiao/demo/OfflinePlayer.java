package com.example.toutiao.demo;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
/** A real, lifecycle-owned player for bundled silent course clips. */
public class OfflinePlayer extends TextureView implements TextureView.SurfaceTextureListener {
 private MediaPlayer player; private Surface surface; private int resource, position; private boolean wanted,prepared;
 public OfflinePlayer(Context c){super(c);init();}
 public OfflinePlayer(Context c, AttributeSet a){super(c,a);init();}
 private void init(){setSurfaceTextureListener(this);setOpaque(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 public void setResource(int id){if(resource==id)return;release();position=0;resource=id;}
 public void play(){wanted=true;if(player==null)open();else if(prepared)player.start();}
 public void pause(){wanted=false;if(player!=null&&prepared)player.pause();}
 public boolean isPlaying(){return player!=null&&prepared&&player.isPlaying();}
 public int progress(){return player!=null&&prepared&&player.getDuration()>0?player.getCurrentPosition()*100/player.getDuration():0;}
 public void release(){wanted=false;if(player!=null){if(prepared)position=player.getCurrentPosition();player.release();player=null;}prepared=false;if(surface!=null){surface.release();surface=null;}}
 private void open(){if(!isAvailable()||resource==0||!wanted)return;
  try(AssetFileDescriptor fd=getResources().openRawResourceFd(resource)){
   player=new MediaPlayer();surface=new Surface(getSurfaceTexture());player.setSurface(surface);
   player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());player.setLooping(true);
   player.setOnPreparedListener(p->{if(player!=p)return;prepared=true;fitVideo(p.getVideoWidth(),p.getVideoHeight());p.seekTo(position);if(wanted)p.start();});
   player.setOnErrorListener((p,w,e)->{release();android.widget.Toast.makeText(getContext(),"样片暂时无法播放，点击重试",android.widget.Toast.LENGTH_SHORT).show();return true;});
   player.prepareAsync();
  }catch(Exception e){release();android.widget.Toast.makeText(getContext(),"无法打开离线样片",android.widget.Toast.LENGTH_SHORT).show();}
 }
 private void fitVideo(int videoWidth,int videoHeight){
  if(videoWidth==0||videoHeight==0||getWidth()==0||getHeight()==0)return;
  float scale=Math.max((float)getWidth()/videoWidth,(float)getHeight()/videoHeight);
  android.graphics.Matrix matrix=new android.graphics.Matrix();
  matrix.setScale(videoWidth*scale/getWidth(),videoHeight*scale/getHeight(),getWidth()/2f,getHeight()/2f);setTransform(matrix);
 }
 public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){open();}
 public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){if(player!=null&&prepared)fitVideo(player.getVideoWidth(),player.getVideoHeight());}
 public boolean onSurfaceTextureDestroyed(SurfaceTexture s){release();return true;}
 public void onSurfaceTextureUpdated(SurfaceTexture s){}
 @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
}
