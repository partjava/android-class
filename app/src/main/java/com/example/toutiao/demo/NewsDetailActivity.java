package com.example.toutiao.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 新闻详情页。
 * 首页列表点击某条新闻后跳到这里，标题、来源、配图、正文都由 Intent 传过来。
 */
public class NewsDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        //取出首页传过来的数据
        String title = getIntent().getStringExtra("title");
        String info = getIntent().getStringExtra("info");
        String content = getIntent().getStringExtra("content");
        int img = getIntent().getIntExtra("img", 0);
        int type = getIntent().getIntExtra("type", News.TYPE_TEXT);

        TextView tvTitle = findViewById(R.id.tv_news_title);
        TextView tvInfo = findViewById(R.id.tv_news_info);
        TextView tvContent = findViewById(R.id.tv_news_content);
        ImageView ivPic = findViewById(R.id.iv_news_pic);
        ImageView ivBack = findViewById(R.id.iv_news_back);

        tvTitle.setText(title);
        tvInfo.setText(info);

        //纯文字新闻没有配图，这时候隐藏 ImageView
        if (type != News.TYPE_TEXT && img != 0) {
            ivPic.setImageResource(img);
            ivPic.setVisibility(View.VISIBLE);
        } else {
            ivPic.setVisibility(View.GONE);
        }

        //万一哪条数据没写正文，这里兜个底，免得详情页空着
        if (content == null || content.isEmpty()) {
            content = "　　" + title + "\n\n　　" + info
                    + "\n\n　　（本条为课程演示数据，完整报道略。）";
        }
        tvContent.setText(content);

        ContentStore store = new ContentStore(this);
        org.json.JSONObject article = ContentStore.article(title, info, content, img, type);
        store.put("history", article);
        android.widget.LinearLayout actions = new android.widget.LinearLayout(this);
        android.widget.Button save = new android.widget.Button(this);
        save.setText(store.contains("saved", title) ? "已收藏 · 点击取消" : "收藏文章");
        save.setOnClickListener(v -> {
            if (store.contains("saved", title)) store.remove("saved", title); else store.put("saved", article);
            save.setText(store.contains("saved", title) ? "已收藏 · 点击取消" : "收藏文章");
        });
        android.widget.Button share = new android.widget.Button(this);
        share.setText("分享");
        share.setOnClickListener(v -> startActivity(android.content.Intent.createChooser(
                new android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_TEXT, title + "\n" + tvContent.getText()), "分享文章")));
        actions.addView(save, new android.widget.LinearLayout.LayoutParams(0,-2,1));
        actions.addView(share, new android.widget.LinearLayout.LayoutParams(0,-2,1));
        ((android.widget.LinearLayout)tvContent.getParent()).addView(actions);

        //左上角返回箭头关闭当前页面
        ivBack.setOnClickListener(v -> finish());
    }
}
