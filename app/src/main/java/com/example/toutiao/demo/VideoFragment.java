package com.example.toutiao.demo;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class VideoFragment extends PageFragment {
    @Override public View onCreateView(android.view.LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_video, parent, false);
    }

    private static final String[] TAB_LABELS = {"推荐", "小视频", "影视", "音乐"};
    private static final int DEFAULT_TAB = 0;

    private ViewPager2 vpChannels;
    private LinearLayout llChrome;
    private LinearLayout topBar;
    private View vFeedScrim;
    private HorizontalScrollView hsTabs;
    private LinearLayout llTabs;

    private final List<View> tabViews = new ArrayList<>();

    private final List<List<VideoItem>> channelData = new ArrayList<>();

    private VideoChannelAdapter channelAdapter;

    private VideoFeedAdapter feedAdapter;

    private int currentTab = DEFAULT_TAB;

    private boolean overFeed;

    private int rippleLightRes;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabViews.clear();
        channelData.clear();
        overFeed = false;
        bindView();
        initVideoData();
        setupTabs();
        bindEvent();

        measureChrome();

        int restored = savedInstanceState == null ? currentTab : savedInstanceState.getInt("channel", 0);
        vpChannels.setCurrentItem(restored, false);
        applyChrome(restored == 0 ? 1f : 0f);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (feedAdapter != null) feedAdapter.onHostPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (feedAdapter != null) feedAdapter.onHostResume();

    }

    @Override
    public void onDestroyView() {
        if (feedAdapter != null) feedAdapter.release();
        super.onDestroyView();
    }

    private void bindView() {
        vpChannels = findViewById(R.id.vp_channels);
        llChrome = findViewById(R.id.ll_chrome);
        topBar = findViewById(R.id.top_bar);
        vFeedScrim = findViewById(R.id.v_feed_scrim);
        hsTabs = findViewById(R.id.hs_video_tabs);
        llTabs = findViewById(R.id.ll_video_tabs);
    }

    private void initVideoData() {
        // 频道数据统一来自 VideoCatalog（mock 种子或上次接口缓存）。
        // 目录列表构造后引用稳定：接口刷新只写缓存、下次启动生效，
        // 这里不做会话内重建（ViewPager2 分页与 feed 适配器持有同一实例）。
        VideoCatalog catalog = VideoCatalog.get(requireContext());
        channelData.add(catalog.recommend());
        channelData.add(catalog.small());
        channelData.add(catalog.movie());
        channelData.add(catalog.music());
        catalog.refreshAsync(requireContext(), null);

        VideoAdapter.OnItemClickListener openDetail = item -> {
            Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
            intent.putExtra(VideoDetailActivity.EXTRA_TITLE, item.getTitle());
            intent.putExtra(VideoDetailActivity.EXTRA_SOURCE, item.getSource());
            intent.putExtra(VideoDetailActivity.EXTRA_DURATION, item.getDuration());
            intent.putExtra(VideoDetailActivity.EXTRA_PLAY_COUNT, item.getPlayCount());
            intent.putExtra(VideoDetailActivity.EXTRA_DESC, item.getDesc());
            intent.putExtra(VideoDetailActivity.EXTRA_COVER, item.getCoverRes());
            startActivity(intent);
        };

        feedAdapter = new VideoFeedAdapter(channelData.get(DEFAULT_TAB));
        channelAdapter = new VideoChannelAdapter(channelData, openDetail, feedAdapter);
        vpChannels.setAdapter(channelAdapter);

        vpChannels.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            
            @Override
            public void onPageSelected(int position) {
                highlightTab(position);
                scrollTabIntoView(position);
            }

            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
                float t = (position == DEFAULT_TAB) ? 1f - offset : 0f;

                applyChrome(Math.max(0f, Math.min(1f, t)));
            }
        });
    }

    private void setupTabs() {
        cacheTabRipples();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < TAB_LABELS.length; i++) {
            View tab = inflater.inflate(R.layout.item_channel_tab, llTabs, false);
            ((TextView) tab.findViewById(R.id.tv_tab_label)).setText(TAB_LABELS[i]);
            final int index = i;
            tab.setOnClickListener(v -> selectTab(index));
            llTabs.addView(tab);
            tabViews.add(tab);
        }

        highlightTab(DEFAULT_TAB);
    }

    private void cacheTabRipples() {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);

        rippleLightRes = tv.resourceId;
    }

    private void selectTab(int index) {
        vpChannels.setCurrentItem(index, true);
    }

    private void highlightTab(int index) {
        currentTab = index;
        int selectedColor = overFeed ? R.color.text_on_dark : R.color.brand_red;
        int normalColor = overFeed ? R.color.text_on_dark_secondary : R.color.text_primary;
        for (int i = 0; i < tabViews.size(); i++) {
            boolean selected = i == index;
            TextView tv = tabViews.get(i).findViewById(R.id.tv_tab_label);
            View line = tabViews.get(i).findViewById(R.id.v_tab_line);
            tv.setTextColor(ContextCompat.getColor(requireContext(), selected ? selectedColor : normalColor));
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            line.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void scrollTabIntoView(int index) {
        View tab = tabViews.get(index);
        hsTabs.post(() -> hsTabs.smoothScrollTo(
                Math.max(0, (tab.getLeft() + tab.getRight()) / 2 - hsTabs.getWidth() / 2), 0));
    }

    private void applyChrome(float t) {

        topBar.setAlpha(1f - t);

        topBar.setVisibility(t >= 0.5f ? View.INVISIBLE : View.VISIBLE);

        vFeedScrim.setAlpha(Math.min(1f, t * 2f));

        boolean wantOverFeed = t >= 0.5f;
        if (wantOverFeed != overFeed) {
            overFeed = wantOverFeed;
            applyTabStyle();

            if (feedAdapter != null) feedAdapter.setActive(overFeed);
        }
    }

    private void applyTabStyle() {

        hsTabs.setBackgroundColor(overFeed ? Color.TRANSPARENT
                : ContextCompat.getColor(requireContext(), R.color.bg_surface));

        for (View tab : tabViews) {

            tab.setBackground(ripple(overFeed ? R.drawable.bg_ripple_on_dark : rippleLightRes));
        }

        highlightTab(currentTab);
    }

    private Drawable ripple(int resId) {
        return resId == 0 ? null : ContextCompat.getDrawable(requireContext(), resId);
    }

    private void measureChrome() {
        final View root = findViewById(R.id.root_video);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> applyChromePadding());
        llChrome.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> applyChromePadding());
    }

    private void applyChromePadding() {
        int padHorizontal = getResources().getDimensionPixelSize(R.dimen.space_m);
        channelAdapter.setGridPadding(llChrome.getHeight(), 0, padHorizontal);
    }

    @Override public void onSaveInstanceState(Bundle out) {
        out.putInt("channel", currentTab);
        super.onSaveInstanceState(out);
    }
    private void bindEvent() {
        findViewById(R.id.tv_search_hint).setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(requireContext());
            input.setHint("输入标题关键词");
            new android.app.AlertDialog.Builder(requireContext()).setTitle("搜索视频").setView(input)
                .setPositiveButton("搜索", (d,w) -> {
                    java.util.List<VideoItem> results = new java.util.ArrayList<>();
                    String key = input.getText().toString().trim();
                    for (java.util.List<VideoItem> group : channelData)
                        for (VideoItem item : group) if (item.getTitle().contains(key)) results.add(item);
                    if (results.isEmpty()) { Toast.makeText(requireContext(), "没有找到相关视频", Toast.LENGTH_SHORT).show(); return; }
                    String[] titles = new String[results.size()];
                    for (int i=0;i<titles.length;i++) titles[i]=results.get(i).getTitle();
                    new android.app.AlertDialog.Builder(requireContext()).setTitle("搜索结果")
                        .setItems(titles, (dialog,index) -> {
                            VideoItem item = results.get(index);
                            Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
                            intent.putExtra("title",item.getTitle()); intent.putExtra("source",item.getSource());
                            intent.putExtra("cover",item.getCoverRes()); intent.putExtra("desc",item.getDesc());
                            startActivity(intent);
                        }).setNegativeButton("关闭",null).show();
                }).setNegativeButton("取消",null).show();
        });
    }
}
