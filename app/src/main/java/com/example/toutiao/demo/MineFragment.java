package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MineFragment extends PageFragment {

    private static final int TAB_WORKS = 0;
    private static final int TAB_COLLECT = 1;
    private static final int TAB_LIKES = 2;

    private static final int CHIP_ALL = 0;
    private static final int CHIP_VIDEO = 1;
    private static final int CHIP_MICRO = 2;

    private int currentTab = TAB_WORKS;
    private int currentChip = CHIP_ALL;
    private String searchFilter = "";

    private TextView tvWorksTitle;
    private TextView tvCollectTab;
    private TextView tvLikeTab;
    private LinearLayout llWorkChips;
    private TextView btnAllWork;
    private TextView btnVideoWork;
    private TextView btnMicroWork;
    private TextView tvWorksCount;
    private LinearLayout llWorksContainer;

    private final List<VideoItem> userVideos = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_mine, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);

        initViews();
        initDefaultData();

        // 设置
        click(R.id.iv_setting, () -> startActivity(new Intent(requireContext(), SettingsActivity.class)));

        // 个人资料相关入口（头像、昵称、小铅笔图标、简介、编辑资料、标签）
        for (int id : new int[]{
                R.id.iv_avatar,
                R.id.tv_profile_name,
                R.id.iv_edit_name,
                R.id.tv_profile_intro,
                R.id.tv_apply_auth,
                R.id.tv_tag_edit
        }) {
            click(id, () -> startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        }

        // IP属地
        click(R.id.tv_tag_ip, this::showIpDialog);

        // 关注 / 粉丝 / 获赞 统计项
        click(R.id.tv_stat_follow, this::showFollowingDialog);
        click(R.id.tv_stat_fans, this::showFansDialog);
        click(R.id.tv_stat_likes, this::showLikesDialog);

        // 消息与聊天室入口
        for (int id : new int[]{
                R.id.iv_msg,
                R.id.ll_msg_private,
                R.id.iv_friend,
                R.id.ll_service,
                R.id.btn_go_comment,
                R.id.tv_chatroom_title,
                R.id.tv_chatroom_more,
                R.id.ll_chatroom_card
        }) {
            click(id, () -> startActivity(new Intent(requireContext(), MsgActivity.class)));
        }

        // 浏览历史与书架
        for (int id : new int[]{R.id.ll_history, R.id.ll_book}) {
            click(id, () -> ContentLibraryActivity.open(requireContext(), "history"));
        }

        // 创作中心快捷入口
        click(R.id.ll_create, () -> ContentLibraryActivity.open(requireContext(), "posts"));

        // 收藏快捷入口
        click(R.id.ll_collect, () -> selectTab(TAB_COLLECT));

        // 快捷发布
        click(R.id.btn_publish, () -> ContentLibraryActivity.compose(requireContext()));

        // 商城购物车与本地订单
        click(R.id.ll_shop, () -> ShoppingDialogs.showCart(requireContext()));
        click(R.id.ll_refund, () -> ShoppingDialogs.showOrders(requireContext()));

        // “作品 / 收藏 / 赞过” Tab 切换（页内直接展示对应内容，不跳转到其他页面）
        click(R.id.tv_works_title, () -> selectTab(TAB_WORKS));
        click(R.id.tv_collect_tab, () -> selectTab(TAB_COLLECT));
        click(R.id.tv_like_tab, () -> selectTab(TAB_LIKES));

        // 作品子分类筛选（全部 / 视频 / 微头条）——页内就地筛选，不再跳走
        click(R.id.btn_all_work, () -> selectChip(CHIP_ALL));
        click(R.id.btn_video_work, () -> selectChip(CHIP_VIDEO));
        click(R.id.btn_micro_work, () -> selectChip(CHIP_MICRO));

        // 搜索作品/内容
        click(R.id.iv_work_search, this::showWorksSearchDialog);

        // 作品数量统计行
        click(R.id.tv_works_count, this::renderContent);

        // 我的功能快捷列表
        click(R.id.tv_all_func, () -> new AlertDialog.Builder(requireContext())
                .setTitle("我的功能")
                .setItems(new String[]{"浏览历史", "收藏", "本地作品", "购物车", "本地订单"}, (d, w) -> {
                    if (w < 3) {
                        ContentLibraryActivity.open(requireContext(), new String[]{"history", "saved", "posts"}[w]);
                    } else if (w == 3) {
                        ShoppingDialogs.showCart(requireContext());
                    } else {
                        ShoppingDialogs.showOrders(requireContext());
                    }
                }).show());

        renderContent();
    }

    private void initViews() {
        tvWorksTitle = findViewById(R.id.tv_works_title);
        tvCollectTab = findViewById(R.id.tv_collect_tab);
        tvLikeTab = findViewById(R.id.tv_like_tab);
        llWorkChips = findViewById(R.id.ll_work_chips);
        btnAllWork = findViewById(R.id.btn_all_work);
        btnVideoWork = findViewById(R.id.btn_video_work);
        btnMicroWork = findViewById(R.id.btn_micro_work);
        tvWorksCount = findViewById(R.id.tv_works_count);
        llWorksContainer = findViewById(R.id.ll_works_container);
    }

    private void initDefaultData() {
        if (userVideos.isEmpty()) {
            userVideos.add(new VideoItem(
                    "自制Vlog · 武汉东湖樱花园春季漫步",
                    "凝墨",
                    "00:08",
                    "1.2万次播放",
                    "春天东湖樱花园落樱缤纷，记录漫步花海的美好瞬间，本片为本地自制课程视频样片。",
                    R.drawable.image2,
                    86,
                    12
            ));
            userVideos.add(new VideoItem(
                    "课程作品 · Android 移动应用开发实战",
                    "凝墨",
                    "00:08",
                    "8600次播放",
                    "今日头条Demo项目实战作品分享，包含Fragment架构与离线组件设计。",
                    R.drawable.image1,
                    42,
                    6
            ));
        }

        // 确保默认有4篇微头条作品，与2个视频共同组成初始的“6个作品”
        ContentStore store = new ContentStore(requireContext());
        if (store.list("posts").length() == 0) {
            store.put("posts", ContentStore.article(
                    "今日打卡：图书馆期末复习第一天，大家一起加油！",
                    "本地微头条 · 06-18 10:30",
                    "今日打卡：图书馆期末复习第一天，大家一起加油！冲刺阶段保持良好作息，争取门门考高分～",
                    R.drawable.image1,
                    News.TYPE_TEXT
            ));
            store.put("posts", ContentStore.article(
                    "分享一组校园雨后的风景照，生活明朗，万物可爱。",
                    "本地微头条 · 06-12 16:45",
                    "分享一组校园雨后的风景照，生活明朗，万物可爱。微风轻拂过树梢，雨后的空气格外清新惬意。",
                    R.drawable.image2,
                    News.TYPE_TEXT
            ));
            store.put("posts", ContentStore.article(
                    "初学Android Studio，终于把这个项目跑通了，很有成就感！",
                    "本地微头条 · 06-05 21:10",
                    "初学Android Studio，终于把Fragment导航和离线视频跑通了，很有成就感！代码世界真的很有趣。",
                    0,
                    News.TYPE_TEXT
            ));
            store.put("posts", ContentStore.article(
                    "今日好天气，微风不燥，适合出去走走散散心～",
                    "本地微头条 · 05-28 15:20",
                    "今日好天气，微风不燥，适合出去走走散散心～享受慢节奏的校园时光。",
                    0,
                    News.TYPE_TEXT
            ));
        }
    }

    private void selectTab(int tab) {
        currentTab = tab;
        renderContent();
    }

    private void selectChip(int chip) {
        currentChip = chip;
        renderContent();
    }

    private void renderContent() {
        if (llWorksContainer == null) return;
        llWorksContainer.removeAllViews();

        Context c = requireContext();
        int brandRed = ContextCompat.getColor(c, R.color.brand_red);
        int textSecondary = ContextCompat.getColor(c, R.color.text_secondary);

        // 1. 刷新大 Tab 高亮状态
        tvWorksTitle.setTextColor(currentTab == TAB_WORKS ? brandRed : textSecondary);
        tvWorksTitle.setTypeface(null, currentTab == TAB_WORKS ? Typeface.BOLD : Typeface.NORMAL);

        tvCollectTab.setTextColor(currentTab == TAB_COLLECT ? brandRed : textSecondary);
        tvCollectTab.setTypeface(null, currentTab == TAB_COLLECT ? Typeface.BOLD : Typeface.NORMAL);

        tvLikeTab.setTextColor(currentTab == TAB_LIKES ? brandRed : textSecondary);
        tvLikeTab.setTypeface(null, currentTab == TAB_LIKES ? Typeface.BOLD : Typeface.NORMAL);

        // 2. 根据 Tab 分支处理
        if (currentTab == TAB_WORKS) {
            llWorkChips.setVisibility(View.VISIBLE);
            updateChipStyles();
            renderWorksList();
        } else if (currentTab == TAB_COLLECT) {
            llWorkChips.setVisibility(View.GONE);
            renderCollectionsList();
        } else {
            llWorkChips.setVisibility(View.GONE);
            renderLikesList();
        }
    }

    private void updateChipStyles() {
        Context c = requireContext();
        int brandRed = ContextCompat.getColor(c, R.color.brand_red);
        int textSecondary = ContextCompat.getColor(c, R.color.text_secondary);

        btnAllWork.setBackgroundResource(currentChip == CHIP_ALL ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        btnAllWork.setTextColor(currentChip == CHIP_ALL ? brandRed : textSecondary);

        btnVideoWork.setBackgroundResource(currentChip == CHIP_VIDEO ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        btnVideoWork.setTextColor(currentChip == CHIP_VIDEO ? brandRed : textSecondary);

        btnMicroWork.setBackgroundResource(currentChip == CHIP_MICRO ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        btnMicroWork.setTextColor(currentChip == CHIP_MICRO ? brandRed : textSecondary);
    }

    private void renderWorksList() {
        ContentStore store = new ContentStore(requireContext());
        JSONArray posts = store.list("posts");

        List<VideoItem> filteredVideos = new ArrayList<>();
        if (currentChip == CHIP_ALL || currentChip == CHIP_VIDEO) {
            for (VideoItem v : userVideos) {
                if (searchFilter.isEmpty() || v.getTitle().contains(searchFilter) || v.getDesc().contains(searchFilter)) {
                    filteredVideos.add(v);
                }
            }
        }

        List<JSONObject> filteredPosts = new ArrayList<>();
        if (currentChip == CHIP_ALL || currentChip == CHIP_MICRO) {
            for (int i = 0; i < posts.length(); i++) {
                JSONObject obj = posts.optJSONObject(i);
                if (obj != null) {
                    String title = obj.optString("title");
                    String content = obj.optString("content");
                    if (searchFilter.isEmpty() || title.contains(searchFilter) || content.contains(searchFilter)) {
                        filteredPosts.add(obj);
                    }
                }
            }
        }

        int totalCount = filteredVideos.size() + filteredPosts.size();
        if (currentChip == CHIP_ALL) {
            tvWorksCount.setText(totalCount + "个作品" + (searchFilter.isEmpty() ? "" : " (搜索过滤)"));
        } else if (currentChip == CHIP_VIDEO) {
            tvWorksCount.setText(filteredVideos.size() + "个视频作品");
        } else {
            tvWorksCount.setText(filteredPosts.size() + "个微头条作品");
        }

        if (totalCount == 0) {
            addEmptyView(llWorksContainer, "暂无相关作品");
            return;
        }

        // 先渲染视频作品卡片
        for (VideoItem video : filteredVideos) {
            llWorksContainer.addView(createVideoCard(video));
        }

        // 再渲染微头条作品卡片
        for (JSONObject post : filteredPosts) {
            llWorksContainer.addView(createMicroPostCard(post, store));
        }
    }

    private void renderCollectionsList() {
        ContentStore store = new ContentStore(requireContext());
        JSONArray savedArticles = store.list("saved");

        VideoStore videoStore = new VideoStore(requireContext());
        List<VideoItem> savedVideos = new ArrayList<>();
        for (VideoItem v : userVideos) {
            if (videoStore.isCollected(v.getId())) {
                savedVideos.add(v);
            }
        }

        int totalCount = savedArticles.length() + savedVideos.size();
        tvWorksCount.setText(totalCount + "个收藏");

        if (totalCount == 0) {
            addEmptyView(llWorksContainer, "暂无收藏内容，去首页收藏精彩资讯吧～");
            return;
        }

        for (VideoItem v : savedVideos) {
            llWorksContainer.addView(createVideoCard(v));
        }

        for (int i = 0; i < savedArticles.length(); i++) {
            JSONObject art = savedArticles.optJSONObject(i);
            if (art != null) {
                llWorksContainer.addView(createArticleCard(art));
            }
        }
    }

    private void renderLikesList() {
        VideoStore videoStore = new VideoStore(requireContext());
        List<VideoItem> likedVideos = new ArrayList<>();
        for (VideoItem v : userVideos) {
            if (videoStore.isLiked(v.getId())) {
                likedVideos.add(v);
            }
        }
        // 如果用户尚未点赞本地自制视频，默认展示第一条视频作为点赞体验示例
        if (likedVideos.isEmpty() && !userVideos.isEmpty()) {
            likedVideos.add(userVideos.get(0));
        }

        tvWorksCount.setText(likedVideos.size() + "个赞过内容");

        if (likedVideos.isEmpty()) {
            addEmptyView(llWorksContainer, "暂无赞过的内容");
            return;
        }

        for (VideoItem v : likedVideos) {
            llWorksContainer.addView(createVideoCard(v));
        }
    }

    private View createVideoCard(VideoItem item) {
        Context c = requireContext();
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(12);
        int padV = dp(10);
        row.setPadding(padH, padV, padH, padV);
        row.setBackgroundResource(R.drawable.bg_mine_card);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(10);
        row.setLayoutParams(rowLp);

        // 封面与时长标签
        FrameLayout thumbBox = new FrameLayout(c);
        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(dp(112), dp(68));
        thumbBox.setLayoutParams(thumbLp);

        ImageView cover = new ImageView(c);
        cover.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setImageResource(item.getCoverRes());
        thumbBox.addView(cover);

        ImageView playIcon = new ImageView(c);
        FrameLayout.LayoutParams playLp = new FrameLayout.LayoutParams(dp(28), dp(28));
        playLp.gravity = Gravity.CENTER;
        playIcon.setLayoutParams(playLp);
        playIcon.setImageResource(R.drawable.ic_play_circle);
        playIcon.setColorFilter(Color.WHITE);
        thumbBox.addView(playIcon);

        TextView duration = new TextView(c);
        FrameLayout.LayoutParams durLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        durLp.gravity = Gravity.BOTTOM | Gravity.END;
        durLp.rightMargin = dp(4);
        durLp.bottomMargin = dp(4);
        duration.setLayoutParams(durLp);
        duration.setText(item.getDuration());
        duration.setTextColor(Color.WHITE);
        duration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        duration.setPadding(dp(4), dp(1), dp(4), dp(1));
        duration.setBackgroundResource(R.drawable.bg_video_duration);
        thumbBox.addView(duration);

        row.addView(thumbBox);

        // 文本信息
        LinearLayout infoBox = new LinearLayout(c);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoLp.leftMargin = dp(12);
        infoBox.setLayoutParams(infoLp);

        TextView title = new TextView(c);
        title.setText(item.getTitle());
        title.setTextColor(ContextCompat.getColor(c, R.color.text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        infoBox.addView(title);

        TextView stats = new TextView(c);
        stats.setText("自制视频 · " + item.getPlayCount());
        stats.setTextColor(ContextCompat.getColor(c, R.color.text_tertiary));
        stats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statsLp.topMargin = dp(6);
        stats.setLayoutParams(statsLp);
        infoBox.addView(stats);

        row.addView(infoBox);

        // 点击直接播放此视频，不跳走导航
        row.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
            intent.putExtra(VideoDetailActivity.EXTRA_TITLE, item.getTitle());
            intent.putExtra(VideoDetailActivity.EXTRA_SOURCE, item.getSource());
            intent.putExtra(VideoDetailActivity.EXTRA_DURATION, item.getDuration());
            intent.putExtra(VideoDetailActivity.EXTRA_PLAY_COUNT, item.getPlayCount());
            intent.putExtra(VideoDetailActivity.EXTRA_DESC, item.getDesc());
            intent.putExtra(VideoDetailActivity.EXTRA_COVER, item.getCoverRes());
            startActivity(intent);
        });

        return row;
    }

    private View createMicroPostCard(JSONObject post, ContentStore store) {
        Context c = requireContext();
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        card.setPadding(pad, pad, pad, pad);
        card.setBackgroundResource(R.drawable.bg_mine_card);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        // 作者信息
        LinearLayout topRow = new LinearLayout(c);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView avatar = new ImageView(c);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
        avatar.setImageResource(new ProfileStore(c).getAvatarRes());
        topRow.addView(avatar);

        LinearLayout nameBox = new LinearLayout(c);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nbLp.leftMargin = dp(8);
        nameBox.setLayoutParams(nbLp);

        TextView name = new TextView(c);
        name.setText(new ProfileStore(c).get("nickname"));
        name.setTextColor(ContextCompat.getColor(c, R.color.text_primary));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        name.setTypeface(null, Typeface.BOLD);
        nameBox.addView(name);

        TextView time = new TextView(c);
        time.setText(post.optString("info", "本地微头条"));
        time.setTextColor(ContextCompat.getColor(c, R.color.text_tertiary));
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        nameBox.addView(time);

        topRow.addView(nameBox);
        card.addView(topRow);

        // 内容正文
        TextView body = new TextView(c);
        body.setText(post.optString("content", post.optString("title")));
        body.setTextColor(ContextCompat.getColor(c, R.color.text_primary));
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        body.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(8);
        body.setLayoutParams(bodyLp);
        card.addView(body);

        // 附图
        int imgRes = post.optInt("img", 0);
        if (imgRes != 0) {
            ImageView postImg = new ImageView(c);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(140), dp(90));
            imgLp.topMargin = dp(8);
            postImg.setLayoutParams(imgLp);
            postImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            postImg.setImageResource(imgRes);
            card.addView(postImg);
        }

        // 底部互动操作
        LinearLayout botRow = new LinearLayout(c);
        botRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams botLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        botLp.topMargin = dp(10);
        botRow.setLayoutParams(botLp);

        TextView likeTv = new TextView(c);
        likeTv.setText("👍 赞");
        likeTv.setTextColor(ContextCompat.getColor(c, R.color.text_tertiary));
        likeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        botRow.addView(likeTv);

        TextView delTv = new TextView(c);
        delTv.setText("删除");
        delTv.setTextColor(ContextCompat.getColor(c, R.color.brand_red));
        delTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        delLp.leftMargin = dp(24);
        delTv.setLayoutParams(delLp);
        delTv.setOnClickListener(v -> new AlertDialog.Builder(c)
                .setMessage("确定删除该微头条作品吗？")
                .setPositiveButton("删除", (d, w) -> {
                    store.remove("posts", post.optString("title"));
                    renderContent();
                })
                .setNegativeButton("取消", null)
                .show());
        botRow.addView(delTv);

        card.addView(botRow);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(c, NewsDetailActivity.class);
            intent.putExtra("title", post.optString("title"));
            intent.putExtra("info", post.optString("info"));
            intent.putExtra("content", post.optString("content"));
            intent.putExtra("img", post.optInt("img"));
            intent.putExtra("type", post.optInt("type"));
            startActivity(intent);
        });

        return card;
    }

    private View createArticleCard(JSONObject art) {
        Context c = requireContext();
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(12);
        card.setPadding(pad, pad, pad, pad);
        card.setBackgroundResource(R.drawable.bg_mine_card);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        LinearLayout infoBox = new LinearLayout(c);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoBox.setLayoutParams(infoLp);

        TextView title = new TextView(c);
        title.setText(art.optString("title"));
        title.setTextColor(ContextCompat.getColor(c, R.color.text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        infoBox.addView(title);

        TextView info = new TextView(c);
        info.setText(art.optString("info"));
        info.setTextColor(ContextCompat.getColor(c, R.color.text_tertiary));
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams infoSubLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoSubLp.topMargin = dp(6);
        info.setLayoutParams(infoSubLp);
        infoBox.addView(info);

        card.addView(infoBox);

        int imgRes = art.optInt("img", 0);
        if (imgRes != 0) {
            ImageView cover = new ImageView(c);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(90), dp(60));
            imgLp.leftMargin = dp(10);
            cover.setLayoutParams(imgLp);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setImageResource(imgRes);
            card.addView(cover);
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(c, NewsDetailActivity.class);
            intent.putExtra("title", art.optString("title"));
            intent.putExtra("info", art.optString("info"));
            intent.putExtra("content", art.optString("content"));
            intent.putExtra("img", art.optInt("img"));
            intent.putExtra("type", art.optInt("type"));
            startActivity(intent);
        });

        return card;
    }

    private void addEmptyView(ViewGroup container, String hint) {
        TextView tv = new TextView(requireContext());
        tv.setText(hint);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setGravity(Gravity.CENTER);
        int pad = dp(32);
        tv.setPadding(pad, pad, pad, pad);
        container.addView(tv);
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private void click(int id, Runnable action) {
        View v = findViewById(id);
        if (v != null) {
            v.setOnClickListener(view -> action.run());
        }
    }

    private void showIpDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("IP属地说明")
                .setMessage("当前展示IP属地：湖北\n\n该属地根据运营商网络分配节点实时解析，用于保障真实可信的网络交流环境。依据国家相关规定，境内展示到省（直辖市/自治区），不可手动修改。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private void showFollowingDialog() {
        String[] followings = new String[]{
                "央视新闻 (官方认证)",
                "新华社 (官方认证)",
                "人民日报 (官方认证)",
                "头条科技前沿",
                "每日经济观察"
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("我的关注")
                .setItems(followings, (d, w) -> {
                    Toast.makeText(requireContext(), "正在前往 " + followings[w] + " 的最新动态", Toast.LENGTH_SHORT).show();
                    ((MainActivity) requireActivity()).showPage(R.id.nav_home, true);
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showFansDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("我的粉丝 (1)")
                .setMessage("头条官方小助手 (已关注你)\n关注时间：2026-03-01\n\n发微头条或参与评论互动，可以收获更多粉丝关注！")
                .setPositiveButton("发微头条", (d, w) -> ContentLibraryActivity.compose(requireContext()))
                .setNeutralButton("私信助手", (d, w) -> startActivity(new Intent(requireContext(), MsgActivity.class)))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showLikesDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("获赞统计")
                .setMessage("累计获赞：1次\n\n点赞来自您在头条视频与微头条作品下的精彩发言。")
                .setPositiveButton("查看赞过的作品", (d, w) -> selectTab(TAB_LIKES))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showWorksSearchDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("输入作品关键词...");
        input.setSingleLine(true);
        input.setText(searchFilter);
        int pad = dp(16);
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = pad;
        params.rightMargin = pad;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("搜索我的作品")
                .setView(container)
                .setPositiveButton("筛选", (d, w) -> {
                    searchFilter = input.getText().toString().trim();
                    renderContent();
                })
                .setNeutralButton("清除筛选", (d, w) -> {
                    searchFilter = "";
                    renderContent();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        ProfileStore profile = new ProfileStore(requireContext());
        TextView nameTv = findViewById(R.id.tv_profile_name);
        if (nameTv != null) {
            nameTv.setText(profile.get("nickname"));
        }
        String intro = profile.get("intro");
        TextView introTv = findViewById(R.id.tv_profile_intro);
        if (introTv != null) {
            introTv.setText(intro.isEmpty() ? "点击填写个人简介" : intro);
        }
        ImageView avatar = findViewById(R.id.iv_avatar);
        if (avatar != null) {
            avatar.setImageResource(profile.getAvatarRes());
        }
        renderContent();
    }
}
