package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    private TextView tvCacheSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bindView();
        bindEvent();
        findViewById(R.id.item_storage_demo).setOnClickListener(v -> startActivity(new Intent(this, StorageDemoActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCacheSizeDisplay();
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
        tvCacheSize = findViewById(R.id.tv_cache_size);
    }

    private void bindEvent() {
        ivBackSetting.setOnClickListener(v -> finish());
        
        itemScan.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("扫一扫")
                    .setMessage("模拟扫码识别功能：\n支持识别登录二维码、商品链接、名片等信息。")
                    .setPositiveButton("模拟扫描", (d, w) -> Toast.makeText(this, "扫描成功：识别到示例内容", Toast.LENGTH_SHORT).show())
                    .setNegativeButton("取消", null)
                    .show();
        });

        itemEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        itemAccountSafe.setOnClickListener(v -> {
            WebActivity.open(this, "账号与安全中心", "file:///android_asset/web/security.html");
        });

        itemPrivacy.setOnClickListener(v -> {
            WebActivity.open(this, "用户协议与隐私政策", "file:///android_asset/web/privacy.html");
        });

        itemDarkMode.setOnClickListener(v -> {
            String[] modes = {"跟随系统", "始终开启深色模式", "始终开启浅色模式"};
            SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
            int current = sp.getInt("dark_mode_idx", 0);
            new AlertDialog.Builder(this)
                    .setTitle("深色模式")
                    .setSingleChoiceItems(modes, current, (dialog, which) -> {
                        sp.edit().putInt("dark_mode_idx", which).apply();
                        if (which == 1) {
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                        } else if (which == 2) {
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                        } else {
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        }
                        Toast.makeText(this, "已设置深色模式为：" + modes[which], Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        });

        itemBigFont.setOnClickListener(v -> {
            String[] scales = {"标准 (1.0x)", "较大 (1.15x)", "特大 (1.3x)", "老年关怀 (1.5x)"};
            SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
            int current = sp.getInt("big_font_idx", 0);
            new AlertDialog.Builder(this)
                    .setTitle("大字号简易模式")
                    .setSingleChoiceItems(scales, current, (dialog, which) -> {
                        sp.edit().putInt("big_font_idx", which).apply();
                        Toast.makeText(this, "已切换显示模式：" + scales[which], Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        });

        itemFontSize.setOnClickListener(v -> {
            String[] sizes = {"小", "标准", "中", "大", "超大"};
            SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
            int current = sp.getInt("font_size_idx", 1);
            new AlertDialog.Builder(this)
                    .setTitle("正文字号大小")
                    .setSingleChoiceItems(sizes, current, (dialog, which) -> {
                        sp.edit().putInt("font_size_idx", which).apply();
                        Toast.makeText(this, "正文字号已设为：" + sizes[which], Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        });

        itemSlideMode.setOnClickListener(v -> {
            String[] modes = {"全屏滑动切换 (TikTok风格)", "经典列表翻页", "水平卡片滑动"};
            SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
            int current = sp.getInt("slide_mode_idx", 0);
            new AlertDialog.Builder(this)
                    .setTitle("视频滑动模式")
                    .setSingleChoiceItems(modes, current, (dialog, which) -> {
                        sp.edit().putInt("slide_mode_idx", which).apply();
                        Toast.makeText(this, "视频滑动体验已配置：" + modes[which], Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        });

        itemClearCache.setOnClickListener(v -> {
            long totalBytes = getDirSize(getCacheDir()) + getDirSize(getExternalCacheDir()) + getDirSize(getCodeCacheDir());
            String sizeStr = (totalBytes < 1024 * 1024) ?
                    String.format(java.util.Locale.CHINA, "%.2f KB", Math.max(16.0, totalBytes / 1024.0)) :
                    String.format(java.util.Locale.CHINA, "%.2f MB", totalBytes / (1024.0 * 1024.0));

            new AlertDialog.Builder(this)
                    .setTitle("清理应用缓存")
                    .setMessage("当前临时网络图片、离线视频片段与网页缓存约 " + sizeStr + "。\n是否立即清理？")
                    .setPositiveButton("立即清理", (dialog, which) -> {
                        deleteDir(getCacheDir());
                        deleteDir(getExternalCacheDir());
                        try {
                            new android.webkit.WebView(this).clearCache(true);
                        } catch (Exception ignored) {}
                        updateCacheSizeDisplay();
                        Toast.makeText(this, "🎉 缓存清理完毕！已成功释放 " + sizeStr + " 存储空间", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        itemAudioSetting.setOnClickListener(v -> {
            String[] audio = {"杜比全景声 (增强)", "原声还原", "语音增强", "夜间降噪"};
            SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
            int current = sp.getInt("audio_idx", 0);
            new AlertDialog.Builder(this)
                    .setTitle("音频音效配置")
                    .setSingleChoiceItems(audio, current, (dialog, which) -> {
                        sp.edit().putInt("audio_idx", which).apply();
                        Toast.makeText(this, "音效已设为：" + audio[which], Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        });

        itemPlayNetwork.setOnClickListener(v -> {
            String[] netModes = {"仅 Wi-Fi 下自动播放", "所有网络均自动播放", "始终关闭自动播放"};
            SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
            int current = sp.getInt("net_play_idx", 0);
            new AlertDialog.Builder(this)
                    .setTitle("移动网络播放策略")
                    .setSingleChoiceItems(netModes, current, (dialog, which) -> {
                        sp.edit().putInt("net_play_idx", which).apply();
                        Toast.makeText(this, "播放网络偏好已保存：" + netModes[which], Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .show();
        });

        itemPushNotify.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("推送通知设置")
                    .setMessage("· 互动通知 (关注、点赞、私信)：开启\n· 推荐优质新闻推送：开启\n· 夜间免打扰 (23:00 - 07:00)：开启")
                    .setPositiveButton("确定", null)
                    .show();
        });

        itemSafeBrowse.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("安全浏览防护")
                    .setMessage("恶意网址拦截已开启\n虚假信息与钓鱼防护等级：高\n下载安全检测：正常保护中")
                    .setPositiveButton("我知道了", null)
                    .show();
        });

        itemCover.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("视频封面与动效")
                    .setMessage("自动截取首帧精彩动图作为封面。\n当前状态：智能匹配模式。")
                    .setPositiveButton("确定", null)
                    .show();
        });

        itemPrivacySimple.setOnClickListener(v -> {
            WebActivity.open(this, "隐私政策与个人信息保护", "file:///android_asset/web/privacy.html");
        });

        itemPersonalInfo.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("个人信息收集清单")
                    .setMessage("收集项目：\n1. 账号基础信息（用户名、头像）\n2. 互动历史（点赞收藏、私信草稿）\n3. 离线播放进度\n所有数据均加密存放在本地应用私有目录。")
                    .setPositiveButton("关闭", null)
                    .show();
        });

        itemThirdShare.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("第三方共享清单")
                    .setMessage("当前版本为纯离线单机教学演示版，无任何第三方 SDK 或外部广告共享行为。")
                    .setPositiveButton("确定", null)
                    .show();
        });

        itemCheckVersion.setOnClickListener(v -> {
            Toast.makeText(this, "正在检查更新...", Toast.LENGTH_SHORT).show();
            v.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    new AlertDialog.Builder(this)
                            .setTitle("检查版本更新")
                            .setMessage("当前版本：v2.4.0 (Build 2026.09)\n已是最新演示版本，无需更新！")
                            .setPositiveButton("太棒了", null)
                            .show();
                }
            }, 500);
        });

        itemAbout.setOnClickListener(v -> {
            WebActivity.open(this, "关于晴川", "file:///android_asset/web/about.html");
        });

        //退出登录
        tvLogout.setOnClickListener(v -> {
            getSharedPreferences("session", MODE_PRIVATE).edit().putBoolean("logged_in", false).apply();
            Toast.makeText(SettingsActivity.this, "已退出登录", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SettingsActivity.this, LoginOneKeyActivity.class);
            //清空整个返回栈。只写 finish() 的话首页、我的这些页面还留在栈里，
            //退出登录后按返回键还能回到应用内部。
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // These preferences persist locally
        bindSwitch(switchReadMode, "read_mode", "阅读模式");
        bindSwitch(switchTipSound, "tip_sound", "提示音效");
        bindSwitch(switchWatermark, "watermark", "原创内容水印");
        bindSwitch(switchScreenShare, "screen_share", "屏幕共享投屏");
        bindSwitch(switchQuickComment, "quick_comment", "快捷评论浮窗");
        bindSwitch(switchRecommendDiscuss, "recommend_discuss", "推荐频道讨论");
        bindSwitch(switchAutoEmoji, "auto_emoji", "自动联想表情包");
    }

    private void bindSwitch(Switch toggle, String key, String label) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        toggle.setChecked(prefs.getBoolean(key, toggle.isChecked()));
        toggle.setContentDescription(label + "开关");
        toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            Toast.makeText(this, (checked ? "已开启：" : "已关闭：") + label, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCacheSizeDisplay() {
        long totalBytes = getDirSize(getCacheDir()) + getDirSize(getExternalCacheDir()) + getDirSize(getCodeCacheDir());
        String formatted;
        if (totalBytes <= 0) {
            formatted = "0.00 MB";
        } else if (totalBytes < 1024 * 1024) {
            formatted = String.format(java.util.Locale.CHINA, "%.2f KB", totalBytes / 1024.0);
        } else {
            formatted = String.format(java.util.Locale.CHINA, "%.2f MB", totalBytes / (1024.0 * 1024.0));
        }
        if (tvCacheSize != null) tvCacheSize.setText(formatted);
    }

    private long getDirSize(java.io.File dir) {
        long size = 0;
        if (dir == null || !dir.exists()) return 0;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.isDirectory()) size += getDirSize(f);
                else size += f.length();
            }
        }
        return size;
    }

    private void deleteDir(java.io.File dir) {
        if (dir == null || !dir.exists()) return;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
    }
}
