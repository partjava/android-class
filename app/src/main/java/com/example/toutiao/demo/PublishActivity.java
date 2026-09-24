package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

public class PublishActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "extra_mode";
    public static final int MODE_MICRO = 0;
    public static final int MODE_ARTICLE = 1;
    public static final int MODE_VIDEO = 2;
    public static final int MODE_QA = 3;

    private int currentMode = MODE_MICRO;

    private ImageView ivCancel;
    private TextView tvModeMicro, tvModeArticle, tvModeVideo;
    private Button btnSubmit;

    private EditText etTitle;
    private View vTitleDivider;
    private EditText etContent;
    private TextView tvWordCount;

    private LinearLayout llPhotoPreviews;
    private LinearLayout llAddPhotoSlot;

    private TextView tvLocation;
    private TextView tvPermission;

    private ImageView ivToolPhoto, ivToolTopic, ivToolLocation;
    private TextView tvToolDraft;

    private final List<Integer> selectedPhotos = new ArrayList<>();
    private int locationIndex = 0;
    private final String[] locations = new String[]{
            "📍 北京市·海淀区",
            "📍 深圳市·南山区",
            "📍 上海市·陆家嘴",
            "📍 广州市·天河区",
            "📍 不显示位置"
    };

    private int permissionIndex = 0;
    private final String[] permissions = new String[]{
            "👥 公开 · 所有人可见",
            "🔒 仅自己可见",
            "⭐ 仅粉丝可见"
    };

    private static final int[] PHOTO_CANDIDATES = new int[]{
            R.drawable.news_smart_city,
            R.drawable.news_space_rocket,
            R.drawable.news_tech_chip,
            R.drawable.news_green_energy,
            R.drawable.news_train_speed,
            R.drawable.news_nature_park,
            R.drawable.news_museum_art,
            R.drawable.news_sports_field,
            R.drawable.news_education_youth,
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3
    };

    public static void start(Context context, int mode) {
        Intent intent = new Intent(context, PublishActivity.class);
        intent.putExtra(EXTRA_MODE, mode);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        currentMode = getIntent().getIntExtra(EXTRA_MODE, MODE_MICRO);

        initViews();
        bindEvents();
        switchMode(currentMode);
    }

    private void initViews() {
        ivCancel = findViewById(R.id.iv_publish_cancel);
        tvModeMicro = findViewById(R.id.tv_mode_micro);
        tvModeArticle = findViewById(R.id.tv_mode_article);
        tvModeVideo = findViewById(R.id.tv_mode_video);
        btnSubmit = findViewById(R.id.btn_publish_submit);

        etTitle = findViewById(R.id.et_publish_title);
        vTitleDivider = findViewById(R.id.v_title_divider);
        etContent = findViewById(R.id.et_publish_content);
        tvWordCount = findViewById(R.id.tv_word_count);

        llPhotoPreviews = findViewById(R.id.ll_photo_previews);
        llAddPhotoSlot = findViewById(R.id.ll_add_photo_slot);

        tvLocation = findViewById(R.id.tv_publish_location);
        tvPermission = findViewById(R.id.tv_publish_permission);

        ivToolPhoto = findViewById(R.id.iv_tool_photo);
        ivToolTopic = findViewById(R.id.iv_tool_topic);
        ivToolLocation = findViewById(R.id.iv_tool_location);
        tvToolDraft = findViewById(R.id.tv_tool_draft);
    }

    private void bindEvents() {
        ivCancel.setOnClickListener(v -> finish());

        tvModeMicro.setOnClickListener(v -> switchMode(MODE_MICRO));
        tvModeArticle.setOnClickListener(v -> switchMode(MODE_ARTICLE));
        tvModeVideo.setOnClickListener(v -> switchMode(MODE_VIDEO));

        btnSubmit.setOnClickListener(v -> doPublish());

        etContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                tvWordCount.setText(len + " / 1000");
                updateSubmitButtonState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        llAddPhotoSlot.setOnClickListener(v -> showPhotoPicker());
        ivToolPhoto.setOnClickListener(v -> showPhotoPicker());

        // 话题点击追加
        bindTopicChip(R.id.chip_topic_1, "#深中通道世界奇迹#");
        bindTopicChip(R.id.chip_topic_2, "#中国空间站科研突破#");
        bindTopicChip(R.id.chip_topic_3, "#科技数码前沿#");
        bindTopicChip(R.id.chip_topic_4, "#大国工匠的日常#");
        bindTopicChip(R.id.chip_topic_5, "#今天吃什么#");
        bindTopicChip(R.id.chip_topic_6, "#生活碎碎念#");

        ivToolTopic.setOnClickListener(v -> insertTopic("#今日新鲜事#"));

        tvLocation.setOnClickListener(v -> cycleLocation());
        ivToolLocation.setOnClickListener(v -> cycleLocation());

        tvPermission.setOnClickListener(v -> cyclePermission());

        tvToolDraft.setOnClickListener(v -> {
            String text = etContent.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "草稿内容为空", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "已保存到本地草稿箱", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindTopicChip(int id, String topic) {
        View chip = findViewById(id);
        if (chip != null) {
            chip.setOnClickListener(v -> insertTopic(topic));
        }
    }

    private void switchMode(int mode) {
        this.currentMode = mode;

        int activeColor = ContextCompat.getColor(this, R.color.brand_red);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        tvModeMicro.setTextColor(mode == MODE_MICRO || mode == MODE_QA ? activeColor : inactiveColor);
        tvModeMicro.setText(mode == MODE_QA ? "发起问答" : "发微头条");

        tvModeArticle.setTextColor(mode == MODE_ARTICLE ? activeColor : inactiveColor);
        tvModeVideo.setTextColor(mode == MODE_VIDEO ? activeColor : inactiveColor);

        boolean needTitle = (mode == MODE_ARTICLE || mode == MODE_QA);
        etTitle.setVisibility(needTitle ? View.VISIBLE : View.GONE);
        vTitleDivider.setVisibility(needTitle ? View.VISIBLE : View.GONE);

        if (mode == MODE_ARTICLE) {
            etTitle.setHint("填写标题会有更多推荐（5-30字）...");
            etContent.setHint("正文内容越丰富详实，越容易获得首页精选推荐...");
        } else if (mode == MODE_VIDEO) {
            etTitle.setVisibility(View.VISIBLE);
            vTitleDivider.setVisibility(View.VISIBLE);
            etTitle.setHint("输入视频主题标题...");
            etContent.setHint("添加视频说明与精彩看点...");
        } else if (mode == MODE_QA) {
            etTitle.setHint("写下你的具体问题（以问号结尾）...");
            etContent.setHint("详细描述问题背景，邀请更多网友参与回答...");
        } else {
            etContent.setHint("分享你的新鲜事、专业见解或生活日常...");
        }
    }

    private void updateSubmitButtonState() {
        boolean hasContent = !etContent.getText().toString().trim().isEmpty();
        btnSubmit.setAlpha(hasContent ? 1.0f : 0.6f);
    }

    private void insertTopic(String topic) {
        int cursor = etContent.getSelectionStart();
        String current = etContent.getText().toString();
        String insertStr = topic + " ";
        if (cursor >= 0 && cursor <= current.length()) {
            etContent.getText().insert(cursor, insertStr);
        } else {
            etContent.append(insertStr);
        }
    }

    private void cycleLocation() {
        locationIndex = (locationIndex + 1) % locations.length;
        tvLocation.setText(locations[locationIndex]);
    }

    private void cyclePermission() {
        permissionIndex = (permissionIndex + 1) % permissions.length;
        tvPermission.setText(permissions[permissionIndex]);
    }

    private void showPhotoPicker() {
        if (selectedPhotos.size() >= 3) {
            Toast.makeText(this, "最多支持添加 3 张精美配图", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("从图库选择精美配图");

        GridView grid = new GridView(this);
        grid.setNumColumns(3);
        grid.setPadding(24, 24, 24, 24);
        grid.setVerticalSpacing(16);
        grid.setHorizontalSpacing(16);

        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return PHOTO_CANDIDATES.length; }
            @Override public Object getItem(int position) { return PHOTO_CANDIDATES[position]; }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                ImageView iv = new ImageView(PublishActivity.this);
                iv.setLayoutParams(new GridView.LayoutParams(200, 200));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageResource(PHOTO_CANDIDATES[position]);
                iv.setBackgroundResource(R.drawable.bg_chip);
                return iv;
            }
        });

        AlertDialog dialog = builder.setView(grid).create();
        grid.setOnItemClickListener((parent, view, position, id) -> {
            int chosenRes = PHOTO_CANDIDATES[position];
            if (!selectedPhotos.contains(chosenRes)) {
                selectedPhotos.add(chosenRes);
                renderPhotoPreviews();
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    private void renderPhotoPreviews() {
        // 保留最后一个 llAddPhotoSlot，移除前面所有的动态缩略图
        llPhotoPreviews.removeViews(0, llPhotoPreviews.getChildCount() - 1);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < selectedPhotos.size(); i++) {
            final int photoRes = selectedPhotos.get(i);
            View thumbItem = inflater.inflate(R.layout.item_publish_photo, llPhotoPreviews, false);
            ImageView ivThumb = thumbItem.findViewById(R.id.iv_preview_thumb);
            ImageView ivRemove = thumbItem.findViewById(R.id.iv_remove_thumb);

            ivThumb.setImageResource(photoRes);
            ivRemove.setOnClickListener(v -> {
                selectedPhotos.remove(Integer.valueOf(photoRes));
                renderPhotoPreviews();
            });

            // 插入在添加按钮前面
            llPhotoPreviews.addView(thumbItem, llPhotoPreviews.getChildCount() - 1);
        }

        llAddPhotoSlot.setVisibility(selectedPhotos.size() >= 3 ? View.GONE : View.VISIBLE);
    }

    private void doPublish() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入要发布的内容", Toast.LENGTH_SHORT).show();
            etContent.requestFocus();
            return;
        }

        String title = etTitle.getText().toString().trim();
        if ((currentMode == MODE_ARTICLE || currentMode == MODE_QA) && title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            etTitle.requestFocus();
            return;
        }

        // 微头条如果没填标题，截取正文前 28 个字符作为列表标题
        if (title.isEmpty()) {
            int end = Math.min(28, content.length());
            title = content.substring(0, end);
            if (content.length() > 28) title += "...";
        }

        String timeStr = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
        String locStr = locations[locationIndex].replace("📍 ", "");
        String info = "本地发布 · " + timeStr + ("不显示位置".equals(locStr) ? "" : " · " + locStr);

        int img1 = selectedPhotos.isEmpty() ? 0 : selectedPhotos.get(0);
        int img2 = selectedPhotos.size() > 1 ? selectedPhotos.get(1) : 0;
        int img3 = selectedPhotos.size() > 2 ? selectedPhotos.get(2) : 0;

        int type;
        if (currentMode == MODE_VIDEO) {
            type = News.TYPE_VIDEO;
            if (img1 == 0) img1 = R.drawable.news_train_speed; // 默认视频封面
        } else if (selectedPhotos.size() >= 3) {
            type = News.TYPE_THREE_IMG;
        } else if (selectedPhotos.size() >= 1) {
            type = News.TYPE_SINGLE_IMG;
        } else {
            type = News.TYPE_TEXT;
        }

        // 1. 存入 ContentStore 的 "posts"（我的作品）
        ContentStore store = new ContentStore(this);
        JSONObject article = ContentStore.article(title, info, content, img1, type);
        store.put("posts", article);

        // 2. 插入到 HomeFragment 推荐信息流顶部
        News newNewsItem = new News(type, title, info, "刚刚", img1, img2, img3).withContent(content);
        HomeFragment.addUserPost(newNewsItem);

        Toast.makeText(this, "🎉 发布成功！已同步至「推荐」与「我的作品」", Toast.LENGTH_SHORT).show();
        finish();
    }
}
