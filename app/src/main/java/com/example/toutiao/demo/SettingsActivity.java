package com.example.toutiao.demo;

        import android.content.Intent;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.ImageView;
        import android.widget.LinearLayout;
        import android.widget.Switch;
        import android.widget.TextView;
        import android.widget.Toast;
        import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private ImageView ivBackSetting;
    private Switch switchReadMode, switchTipSound, switchWatermark, switchScreenShare, switchQuickComment, switchRecommendDiscuss, switchAutoEmoji;
    private LinearLayout itemScan, itemEditProfile, itemAccountSafe, itemPrivacy, itemDarkMode, itemBigFont, itemFontSize, itemSlideMode, itemClearCache, itemAudioSetting, itemPlayNetwork, itemPushNotify, itemSafeBrowse, itemCover, itemPrivacySimple, itemPersonalInfo, itemThirdShare, itemCheckVersion, itemAbout;
    private TextView tvLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bindView();
        bindEvent();
    }

    private void bindView() {
        ivBackSetting = findViewById(R.id.iv_back_setting);
        switchReadMode = findViewById(R.id.switch_read_mode);
        switchTipSound = findViewById(R.id.switch_tip_sound);
        switchWatermark = findViewById(R.id.switch_watermark);
        switchScreenShare = findViewById(R.id.switch_screen_share);
        switchQuickComment = findViewById(R.id.switch_quick_comment);
        switchRecommendDiscuss = findViewById(R.id.switch_recommend_discuss);
        switchAutoEmoji = findViewById(R.id.switch_auto_emoji);

        itemScan = findViewById(R.id.item_scan);
        itemEditProfile = findViewById(R.id.item_edit_profile);
        itemAccountSafe = findViewById(R.id.item_account_safe);
        itemPrivacy = findViewById(R.id.item_privacy);
        itemDarkMode = findViewById(R.id.item_dark_mode);
        itemBigFont = findViewById(R.id.item_big_font);
        itemFontSize = findViewById(R.id.item_font_size);
        itemSlideMode = findViewById(R.id.item_slide_mode);
        itemClearCache = findViewById(R.id.item_clear_cache);
        itemAudioSetting = findViewById(R.id.item_audio_setting);
        itemPlayNetwork = findViewById(R.id.item_play_network);
        itemPushNotify = findViewById(R.id.item_push_notify);
        itemSafeBrowse = findViewById(R.id.item_safe_browse);
        itemCover = findViewById(R.id.item_cover);
        itemPrivacySimple = findViewById(R.id.item_privacy_simple);
        itemPersonalInfo = findViewById(R.id.item_personal_info);
        itemThirdShare = findViewById(R.id.item_third_share);
        itemCheckVersion = findViewById(R.id.item_check_version);
        itemAbout = findViewById(R.id.item_about);
        tvLogout = findViewById(R.id.tv_logout);
    }

    private void bindEvent() {
        ivBackSetting.setOnClickListener(v -> finish());
        itemScan.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "扫一扫", Toast.LENGTH_SHORT).show());
        itemEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
        itemAccountSafe.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "账号与安全", Toast.LENGTH_SHORT).show());
        itemPrivacy.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "隐私设置", Toast.LENGTH_SHORT).show());
        itemDarkMode.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "深色模式设置", Toast.LENGTH_SHORT).show());
        itemBigFont.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "大字模式", Toast.LENGTH_SHORT).show());
        itemFontSize.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "字体大小", Toast.LENGTH_SHORT).show());
        itemSlideMode.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "图文滑动方式", Toast.LENGTH_SHORT).show());
        itemClearCache.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "正在清理缓存", Toast.LENGTH_SHORT).show());
        itemAudioSetting.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "音频设置", Toast.LENGTH_SHORT).show());
        itemPlayNetwork.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "播放与网络设置", Toast.LENGTH_SHORT).show());
        itemPushNotify.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "推送通知设置", Toast.LENGTH_SHORT).show());
        itemSafeBrowse.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "安全浏览设置", Toast.LENGTH_SHORT).show());
        itemCover.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "头条封面", Toast.LENGTH_SHORT).show());
        itemPrivacySimple.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "隐私政策简明版", Toast.LENGTH_SHORT).show());
        itemPersonalInfo.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "个人信息收集清单", Toast.LENGTH_SHORT).show());
        itemThirdShare.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "第三方信息共享清单", Toast.LENGTH_SHORT).show());
        itemCheckVersion.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "已是最新版本18.4.0", Toast.LENGTH_SHORT).show());
        itemAbout.setOnClickListener(v -> Toast.makeText(SettingsActivity.this, "关于头条", Toast.LENGTH_SHORT).show());

        //退出登录
        tvLogout.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "退出登录成功，返回登录页", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SettingsActivity.this, LoginOneKeyActivity.class);
            //清空整个返回栈。只写 finish() 的话首页、我的这些页面还留在栈里，
            //退出登录后按返回键还能回到应用内部。
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        //开关监听
        switchReadMode.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(SettingsActivity.this, "自动阅读模式："+isChecked, Toast.LENGTH_SHORT).show());
        switchTipSound.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(SettingsActivity.this, "提示音开关："+isChecked, Toast.LENGTH_SHORT).show());
        switchWatermark.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(SettingsActivity.this, "自动加水印："+isChecked, Toast.LENGTH_SHORT).show());
        switchScreenShare.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(SettingsActivity.this, "截屏自动分享："+isChecked, Toast.LENGTH_SHORT).show());
        switchQuickComment.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(SettingsActivity.this, "一键发评："+isChecked, Toast.LENGTH_SHORT).show());
        switchRecommendDiscuss.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(SettingsActivity.this, "推荐相关讨论："+isChecked, Toast.LENGTH_SHORT).show());
        switchAutoEmoji.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(SettingsActivity.this, "自动匹配表情包："+isChecked, Toast.LENGTH_SHORT).show());
    }
}
