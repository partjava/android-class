package com.example.toutiao.demo;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
public final class VideoActions {
 private VideoActions(){}
 public static void share(Context context, VideoItem item){
  Intent i=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,item.getTitle()+"\n本地课程演示样片（不是新闻原视频）\n"+item.getDesc());
  context.startActivity(Intent.createChooser(i,"分享课程样片说明"));
 }
 public static void comments(Context c,VideoItem item,VideoStore store,Runnable changed){
  LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(20*c.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,0);
  TextView history=new TextView(c);history.setTextColor(0xff333333);
  java.util.List<String> comments=store.comments(item.getId());
  history.setText(comments.isEmpty()?"暂无评论，留下你的学习感受吧。":android.text.TextUtils.join("\n\n",comments));
  android.widget.ScrollView scroll=new android.widget.ScrollView(c);scroll.addView(history);box.addView(scroll,new LinearLayout.LayoutParams(-1,(int)(180*c.getResources().getDisplayMetrics().density)));
  EditText input=new EditText(c);input.setHint("评论仅保存在本机");input.setMaxLines(4);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});box.addView(input);
  AlertDialog dialog=new AlertDialog.Builder(c).setTitle("本地评论").setView(box).setNegativeButton("关闭",null).setPositiveButton("发布",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=input.getText().toString().trim();if(text.isEmpty()){input.setError("请输入评论");return;}store.addComment(item.getId(),text);changed.run();dialog.dismiss();}));dialog.show();
 }
}
