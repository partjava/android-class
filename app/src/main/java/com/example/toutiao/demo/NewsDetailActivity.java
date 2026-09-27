package com.example.toutiao.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 新闻详情页。
 * 列表页只传标题/来源/配图这些摘要字段；正文在这里才按需查询：
 * 用户自己发布的动态随 extra 带过来，站内文章从 NewsContentStore
 * 按标题拉取全文——对应“列表接口给摘要、详情接口给全文”的两段式加载。
 */
public class NewsDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        //取出列表页传过来的摘要数据
        String title = getIntent().getStringExtra("title");
        String info = getIntent().getStringExtra("info");
        String content = getIntent().getStringExtra("content");
        int img = getIntent().getIntExtra("img", 0);
        int type = getIntent().getIntExtra("type", News.TYPE_TEXT);
        String imgUrl = getIntent().getStringExtra("img_url");
        String linkUrl = getIntent().getStringExtra("link");

        //正文三级来源：用户发布的动态 extra 里就有；接口头条在运行时层；
        //站内文章在静态文章库——都是进详情页这一刻才查询
        if (content == null || content.isEmpty()) {
            content = NewsContentStore.contentOf(title);
        }

        TextView tvTitle = findViewById(R.id.tv_news_title);
        TextView tvInfo = findViewById(R.id.tv_news_info);
        TextView tvContent = findViewById(R.id.tv_news_content);
        TextView tvReadOriginal = findViewById(R.id.tv_news_read_original);
        ImageView ivPic = findViewById(R.id.iv_news_pic);
        ImageView ivBack = findViewById(R.id.iv_news_back);

        tvTitle.setText(title);
        tvInfo.setText(info);

        //配图三级：接口头条的远程封面 → 本地文章的 drawable → 纯文字无图
        if (imgUrl != null && !imgUrl.isEmpty()) {
            RemoteImage.load(ivPic, imgUrl);
            ivPic.setVisibility(View.VISIBLE);
        } else if (type != News.TYPE_TEXT && img != 0) {
            ivPic.setImageResource(img);
            ivPic.setVisibility(View.VISIBLE);
        } else {
            ivPic.setVisibility(View.GONE);
        }

        //接口头条提供原文链接时显示「阅读原文」，跳系统浏览器看全文
        if (linkUrl != null && !linkUrl.isEmpty()) {
            tvReadOriginal.setVisibility(View.VISIBLE);
            tvReadOriginal.setOnClickListener(v -> startActivity(
                    new android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(linkUrl))));
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
