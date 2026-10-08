package com.example.toutiao.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 新闻详情页。
 * 打破传统单图死格式，采用原生流式图文混排（Rich Media Flow）：
 * 1. 严格遵循官方新闻网真实排版：首段文字（本网讯/导语）在最前，图片不再死板置顶。
 * 2. 支持多图穿插：配图 1 穿插在第一自然段之后，配图 2 穿插在第二自然段之后。
 * 3. 完整显示长篇新闻全部自然段落，排版清爽，支持收藏与分享。
 */
public class NewsDetailActivity extends AppCompatActivity {

    private CommentStore commentStore;
    private ScrollView svNewsScroll;
    private View llCommentsHeader;
    private LinearLayout llCommentsContainer;
    private TextView tvCommentHeaderCount;
    private TextView tvBottomCommentBadge;
    private ImageView ivBottomLike;
    private TextView tvBottomLikeCount;
    private ImageView ivBottomCollect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.WHITE);
            androidx.core.view.WindowInsetsControllerCompat controller =
                    new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(true);
        }

        // 取出列表页传过来的数据
        String title = getIntent().getStringExtra("title");
        String info = getIntent().getStringExtra("info");
        String content = getIntent().getStringExtra("content");
        int img1 = getIntent().getIntExtra("img", 0);
        int img2 = getIntent().getIntExtra("img2", 0);
        int img3 = getIntent().getIntExtra("img3", 0);
        int type = getIntent().getIntExtra("type", News.TYPE_TEXT);
        String imgUrl = getIntent().getStringExtra("img_url");
        String imgUrl2 = getIntent().getStringExtra("img_url_2");
        String imgUrl3 = getIntent().getStringExtra("img_url_3");
        String blocksJson = getIntent().getStringExtra("blocks_json");
        String link = getIntent().getStringExtra("link");

        // 查询正文全文
        if (content == null || content.trim().isEmpty()) {
            content = NewsContentStore.contentOf(getIntent().getStringExtra("news_id"), title);
        }
        if (content == null || content.trim().isEmpty()) {
            content = "　　本网讯（来源：" + (info != null && !info.isEmpty() ? info : "官方发布") + "）\n\n　　"
                    + title + "。\n\n　　相关部门正积极推进各项工作落地见效，进一步提升服务质量与育人成效。";
        }

        TextView tvTitle = findViewById(R.id.tv_news_title);
        TextView tvInfo = findViewById(R.id.tv_news_info);
        LinearLayout llRichContent = findViewById(R.id.ll_rich_content);
        ImageView ivBack = findViewById(R.id.iv_news_back);
        TextView tvFontSize = findViewById(R.id.tv_news_font_size);
        ImageView ivTopShare = findViewById(R.id.iv_news_top_share);

        tvTitle.setText(title);
        tvInfo.setText(info);

        float currentFontSize = getSharedPreferences("user_settings", MODE_PRIVATE).getFloat("news_font_size", 16.5f);

        boolean renderedBlocks = false;
        String detailType = getIntent().getStringExtra("detail_type");
        if (NewsContract.WEB.equals(detailType)) {
            webView = new android.webkit.WebView(this);
            webView.getSettings().setJavaScriptEnabled(false);
            webView.getSettings().setAllowFileAccess(true);
            webView.setWebViewClient(new android.webkit.WebViewClient() {
                @Override public void onReceivedError(android.webkit.WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                    if (request.isForMainFrame()) Toast.makeText(NewsDetailActivity.this, "网页加载失败，请返回重试", Toast.LENGTH_SHORT).show();
                }
            });
            llRichContent.addView(webView, new LinearLayout.LayoutParams(-1, dpToPx(520)));
            String uri = getIntent().getStringExtra("media_uri");
            if (uri != null && (uri.startsWith("file:///android_asset/news/") || uri.startsWith("https://"))) webView.loadUrl(uri);
            renderedBlocks = true;
        } else if (content.trim().startsWith("<")) {
            TextView html = new TextView(this);
            html.setText(android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_LEGACY));
            html.setTextSize(currentFontSize);
            html.setLineSpacing(dpToPx(8), 1f);
            llRichContent.addView(html);
            paragraphViews.add(html);
            renderedBlocks = true;
        }
        if (!renderedBlocks && blocksJson != null && !blocksJson.trim().isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(blocksJson);
                if (arr.length() > 0) {
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject obj = arr.getJSONObject(i);
                        String bType = obj.optString("type");
                        if ("text".equals(bType)) {
                            String pText = obj.optString("text", "").trim();
                            if (!pText.isEmpty()) {
                                TextView tvP = new TextView(this);
                                tvP.setText(pText.startsWith("　　") ? pText : "　　" + pText);
                                tvP.setTextSize(currentFontSize);
                                tvP.setTextColor(getResources().getColor(R.color.text_primary));
                                tvP.setLineSpacing(dpToPx(8), 1.0f);
                                LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                lpText.bottomMargin = dpToPx(14);
                                tvP.setLayoutParams(lpText);
                                llRichContent.addView(tvP);
                                paragraphViews.add(tvP);
                            }
                        } else if ("image".equals(bType)) {
                            String url = obj.optString("url", "").trim();
                            if (!url.isEmpty()) {
                                addImageView(llRichContent, url);
                            }
                        }
                    }
                    renderedBlocks = true;
                }
            } catch (Exception ignored) {
                renderedBlocks = false;
            }
        }

        if (!renderedBlocks) {
            // 普通文章兜底排版
            List<Object> imageList = new ArrayList<>();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                imageList.add(imgUrl);
            } else if (img1 != 0) {
                imageList.add(img1);
            }

            if (imgUrl2 != null && !imgUrl2.isEmpty()) {
                imageList.add(imgUrl2);
            } else if (img2 != 0) {
                imageList.add(img2);
            }

            if (imgUrl3 != null && !imgUrl3.isEmpty()) {
                imageList.add(imgUrl3);
            } else if (img3 != 0) {
                imageList.add(img3);
            }

            // 解析正文段落
            String[] rawParagraphs = content.split("\n\n");
            List<String> paragraphs = new ArrayList<>();
            for (String p : rawParagraphs) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    paragraphs.add(trimmed);
                }
            }

            // 动态流式排版：文字与图片交替穿插
            for (int i = 0; i < paragraphs.size(); i++) {
                String pText = paragraphs.get(i);
                TextView tvP = new TextView(this);
                tvP.setText(pText.startsWith("　　") ? pText : "　　" + pText);
                tvP.setTextSize(currentFontSize);
                tvP.setTextColor(getResources().getColor(R.color.text_primary));
                tvP.setLineSpacing(dpToPx(8), 1.0f);
                LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lpText.bottomMargin = dpToPx(14);
                tvP.setLayoutParams(lpText);
                llRichContent.addView(tvP);
                paragraphViews.add(tvP);

                if (i < imageList.size()) {
                    Object imgSpec = imageList.get(i);
                    addImageView(llRichContent, imgSpec);
                }
            }

            for (int i = paragraphs.size(); i < imageList.size(); i++) {
                Object imgSpec = imageList.get(i);
                addImageView(llRichContent, imgSpec);
            }
        }

        // 底部动作栏：收藏与分享，完整持久化网络配图与图文流
        ContentStore store = new ContentStore(this);
        org.json.JSONObject article = ContentStore.article(title, info, content, img1, type, imgUrl, imgUrl2, imgUrl3, blocksJson, link);
        final String newsId = getIntent().getStringExtra("news_id") == null ? LegacyIds.news(title) : getIntent().getStringExtra("news_id");
        try { article.put("news_id", newsId); article.put("detail_type", detailType == null ? NewsContract.HTML : detailType); article.put("media_uri", getIntent().getStringExtra("media_uri")); article.put("img2",img2); article.put("img3",img3); } catch (Exception ignored) {}
        store.put("history", article);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpActions = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpActions.topMargin = dpToPx(20);
        lpActions.bottomMargin = dpToPx(24);
        actions.setLayoutParams(lpActions);

        android.widget.Button save = new android.widget.Button(this);
        save.setText(store.contains("saved", newsId) ? "已收藏 · 点击取消" : "收藏文章");
        save.setOnClickListener(v -> {
            if (store.contains("saved", newsId)) {
                store.remove("saved", newsId);
            } else {
                store.put("saved", article);
            }
            save.setText(store.contains("saved", newsId) ? "已收藏 · 点击取消" : "收藏文章");
            updateCollectUi(newsId, store);
        });

        android.widget.Button share = new android.widget.Button(this);
        share.setText("分享文章");
        final String finalShareText = content;
        final String finalLink = link;
        share.setOnClickListener(v -> doShareArticle(title, info, finalShareText, finalLink));

        LinearLayout.LayoutParams btnLp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnLp1.rightMargin = dpToPx(8);
        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnLp2.leftMargin = dpToPx(8);

        actions.addView(save, btnLp1);
        actions.addView(share, btnLp2);
        llRichContent.addView(actions);

        // 顶栏按钮事件
        if (tvFontSize != null) {
            tvFontSize.setOnClickListener(v -> showFontSizeDialog());
        }
        if (ivTopShare != null) {
            ivTopShare.setOnClickListener(v -> doShareArticle(title, info, finalShareText, finalLink));
        }

        // 左上角返回
        ivBack.setOnClickListener(v -> finish());

        // 初始化评论与互动组件
        commentStore = new CommentStore(this);
        svNewsScroll = findViewById(R.id.sv_news_scroll);
        llCommentsHeader = findViewById(R.id.ll_comments_header);
        llCommentsContainer = findViewById(R.id.ll_comments_container);
        tvCommentHeaderCount = findViewById(R.id.tv_comment_header_count);
        tvBottomCommentBadge = findViewById(R.id.tv_bottom_comment_badge);
        ivBottomLike = findViewById(R.id.iv_bottom_like);
        tvBottomLikeCount = findViewById(R.id.tv_bottom_like_count);
        ivBottomCollect = findViewById(R.id.iv_bottom_collect);

        renderComments(title);
        if (commentStore.isArticleLiked(title)) store.put("liked", article);
        updateLikeUi(title);
        updateCollectUi(newsId, store);

        // 底部评论输入触发
        View llCommentInputTrigger = findViewById(R.id.ll_comment_input_trigger);
        if (llCommentInputTrigger != null) {
            llCommentInputTrigger.setOnClickListener(v -> showCommentInputDialog(title));
        }

        // 底部评论跳转
        View flBottomCommentBtn = findViewById(R.id.fl_bottom_comment_btn);
        if (flBottomCommentBtn != null) {
            flBottomCommentBtn.setOnClickListener(v -> {
                if (svNewsScroll != null && llCommentsHeader != null) {
                    svNewsScroll.smoothScrollTo(0, llCommentsHeader.getTop());
                }
            });
        }

        // 底部点赞
        View llBottomLikeBtn = findViewById(R.id.ll_bottom_like_btn);
        if (llBottomLikeBtn != null) {
            llBottomLikeBtn.setOnClickListener(v -> {
                boolean wasLiked = commentStore.isArticleLiked(title);
                commentStore.toggleArticleLike(title);
                if (commentStore.isArticleLiked(title)) store.put("liked", article);
                else store.remove("liked", newsId);
                updateLikeUi(title);
                if (ivBottomLike != null) {
                    ivBottomLike.animate().scaleX(1.35f).scaleY(1.35f).setDuration(150)
                            .withEndAction(() -> ivBottomLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                            .start();
                }
                Toast.makeText(this, !wasLiked ? "❤️ 点赞成功 +1" : "已取消点赞", Toast.LENGTH_SHORT).show();
            });
        }

        // 底部收藏
        View flBottomCollectBtn = findViewById(R.id.fl_bottom_collect_btn);
        if (flBottomCollectBtn != null) {
            flBottomCollectBtn.setOnClickListener(v -> {
                if (store.contains("saved", newsId)) {
                    store.remove("saved", newsId);
                    Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
                } else {
                    store.put("saved", article);
                    Toast.makeText(this, "⭐ 已加入我的收藏", Toast.LENGTH_SHORT).show();
                }
                updateCollectUi(newsId, store);
                save.setText(store.contains("saved", newsId) ? "已收藏 · 点击取消" : "收藏文章");
            });
        }

        // 底部分享
        View flBottomShareBtn = findViewById(R.id.fl_bottom_share_btn);
        if (flBottomShareBtn != null) {
            flBottomShareBtn.setOnClickListener(v -> doShareArticle(title, info, finalShareText, finalLink));
        }
    }

    private android.webkit.WebView webView;
    @Override protected void onDestroy() {
        if (webView != null) { webView.stopLoading(); webView.destroy(); webView = null; }
        super.onDestroy();
    }

    private final List<TextView> paragraphViews = new ArrayList<>();

    private void showFontSizeDialog() {
        final String[] items = {"小号字体 (14sp)", "标准字体 (16.5sp)", "大号字体 (19sp)", "特大字体 (22sp)"};
        final float[] sizes = {14.0f, 16.5f, 19.0f, 22.0f};
        int selectedIdx = 1;
        float current = getSharedPreferences("user_settings", MODE_PRIVATE).getFloat("news_font_size", 16.5f);
        for (int i = 0; i < sizes.length; i++) {
            if (Math.abs(sizes[i] - current) < 0.2f) {
                selectedIdx = i;
                break;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("调节阅读字号")
                .setSingleChoiceItems(items, selectedIdx, (dialog, which) -> {
                    float newSize = sizes[which];
                    getSharedPreferences("user_settings", MODE_PRIVATE).edit().putFloat("news_font_size", newSize).apply();
                    for (TextView tv : paragraphViews) {
                        tv.setTextSize(newSize);
                    }
                    android.widget.Toast.makeText(this, "正文字号已设为：" + items[which], android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doShareArticle(String title, String info, String content, String link) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(title).append("】\n");
        if (info != null && !info.trim().isEmpty()) {
            sb.append("来源/时间：").append(info).append("\n");
        }
        if (link != null && !link.trim().isEmpty()) {
            sb.append("官网原文直达：").append(link).append("\n");
        }
        sb.append("\n");
        String snippet = (content != null ? content : "").replace("　", "").replace("\n", " ").trim();
        if (snippet.length() > 160) {
            snippet = snippet.substring(0, 160) + "……";
        }
        sb.append(snippet).append("\n\n(分享自晴川 · 武汉晴川学院校园版)");

        android.content.Intent sendIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
        sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        startActivity(android.content.Intent.createChooser(sendIntent, "分享文章到"));
    }

    private void addImageView(LinearLayout container, Object imgSpec) {
        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpImg.topMargin = dpToPx(6);
        lpImg.bottomMargin = dpToPx(16);
        iv.setLayoutParams(lpImg);
        iv.setAdjustViewBounds(true);
        iv.setMaxHeight(dpToPx(300));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundResource(R.drawable.bg_shop_card);
        iv.setClipToOutline(true);

        if (imgSpec instanceof Integer) {
            iv.setImageResource((Integer) imgSpec);
        } else if (imgSpec instanceof String) {
            RemoteImage.load(iv, (String) imgSpec);
        }

        // 交互优化：点击图片唤起全屏沉浸式大图预览
        iv.setOnClickListener(v -> showImagePreviewDialog(imgSpec));

        container.addView(iv);
    }

    private void showImagePreviewDialog(Object imgSpec) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        android.widget.FrameLayout layout = new android.widget.FrameLayout(this);
        layout.setBackgroundColor(android.graphics.Color.BLACK);

        ImageView previewIv = new ImageView(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        previewIv.setLayoutParams(lp);
        previewIv.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (imgSpec instanceof Integer) {
            previewIv.setImageResource((Integer) imgSpec);
        } else if (imgSpec instanceof String) {
            RemoteImage.load(previewIv, (String) imgSpec);
        }

        TextView tip = new TextView(this);
        tip.setText("轻触任意位置退出原图预览");
        tip.setTextColor(android.graphics.Color.parseColor("#B0FFFFFF"));
        tip.setTextSize(13);
        android.widget.FrameLayout.LayoutParams tipLp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        tipLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        tipLp.bottomMargin = dpToPx(36);
        tip.setLayoutParams(tipLp);

        layout.addView(previewIv);
        layout.addView(tip);

        layout.setOnClickListener(v -> dialog.dismiss());
        previewIv.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(layout);
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void renderComments(String newsTitle) {
        if (llCommentsContainer == null) return;
        llCommentsContainer.removeAllViews();
        List<CommentStore.Comment> list = commentStore.getComments(newsTitle);
        int count = list.size();
        if (tvCommentHeaderCount != null) tvCommentHeaderCount.setText("(" + count + ")");
        if (tvBottomCommentBadge != null) tvBottomCommentBadge.setText(String.valueOf(count));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (CommentStore.Comment c : list) {
            View itemView = inflater.inflate(R.layout.item_news_comment, llCommentsContainer, false);
            ImageView ivAvatar = itemView.findViewById(R.id.iv_comment_avatar);
            TextView tvAuthor = itemView.findViewById(R.id.tv_comment_author);
            TextView tvTag = itemView.findViewById(R.id.tv_comment_tag);
            TextView tvTime = itemView.findViewById(R.id.tv_comment_time);
            TextView tvContent = itemView.findViewById(R.id.tv_comment_content);
            View llLike = itemView.findViewById(R.id.ll_comment_like);
            ImageView ivLike = itemView.findViewById(R.id.iv_comment_like);
            TextView tvLikeCount = itemView.findViewById(R.id.tv_comment_like_count);

            tvAuthor.setText(c.author);
            tvTag.setText(c.tag != null ? c.tag : "晴川认证用户");
            tvTime.setText(c.time);
            tvContent.setText(c.content);
            tvLikeCount.setText(String.valueOf(c.likeCount));

            int[] avatars = {R.drawable.avatar_blue, R.drawable.avatar_green, R.drawable.avatar_orange, R.drawable.avatar_purple};
            int avatarIdx = Math.abs(c.author.hashCode()) % avatars.length;
            ivAvatar.setImageResource(avatars[avatarIdx]);

            updateCommentLikeIcon(ivLike, tvLikeCount, c.isLiked, c.likeCount);

            llLike.setOnClickListener(v -> {
                boolean nowLiked = commentStore.toggleCommentLike(newsTitle, c.id);
                c.isLiked = nowLiked;
                c.likeCount += (nowLiked ? 1 : -1);
                if (c.likeCount < 0) c.likeCount = 0;
                updateCommentLikeIcon(ivLike, tvLikeCount, c.isLiked, c.likeCount);
            });

            llCommentsContainer.addView(itemView);
        }
    }

    private void updateCommentLikeIcon(ImageView ivLike, TextView tvLikeCount, boolean isLiked, int count) {
        ivLike.setColorFilter(ContextCompat.getColor(this,
                isLiked ? R.color.brand_red : R.color.text_tertiary));
        tvLikeCount.setText(String.valueOf(count));
        tvLikeCount.setTextColor(ContextCompat.getColor(this,
                isLiked ? R.color.brand_red : R.color.text_tertiary));
    }

    private void updateLikeUi(String newsTitle) {
        if (ivBottomLike == null || tvBottomLikeCount == null) return;
        boolean liked = commentStore.isArticleLiked(newsTitle);
        int count = commentStore.getArticleLikes(newsTitle);
        ivBottomLike.setColorFilter(ContextCompat.getColor(this,
                liked ? R.color.brand_red : R.color.icon_primary));
        tvBottomLikeCount.setText(String.valueOf(count));
        tvBottomLikeCount.setTextColor(ContextCompat.getColor(this,
                liked ? R.color.brand_red : R.color.text_secondary));
    }

    private void updateCollectUi(String newsTitle, ContentStore store) {
        if (ivBottomCollect == null) return;
        boolean isSaved = store.contains("saved", newsTitle);
        ivBottomCollect.setColorFilter(ContextCompat.getColor(this,
                isSaved ? R.color.brand_red : R.color.icon_primary));
    }

    private void showCommentInputDialog(String newsTitle) {
        final EditText etInput = new EditText(this);
        etInput.setHint("说点什么吧，文明发言交流...");
        etInput.setTextSize(15f);
        etInput.setMinLines(3);
        etInput.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        etInput.setBackgroundResource(R.drawable.bg_search);
        etInput.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(8));
        container.addView(etInput, lp);

        new AlertDialog.Builder(this)
                .setTitle("发表我的讨论")
                .setView(container)
                .setPositiveButton("发布", (dialog, which) -> {
                    String text = etInput.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, "请输入要发表的评论内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    commentStore.addComment(newsTitle, text);
                    renderComments(newsTitle);
                    Toast.makeText(this, "🎉 评论发表成功！", Toast.LENGTH_SHORT).show();
                    if (svNewsScroll != null && llCommentsHeader != null) {
                        svNewsScroll.post(() -> svNewsScroll.smoothScrollTo(0, llCommentsHeader.getTop()));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
