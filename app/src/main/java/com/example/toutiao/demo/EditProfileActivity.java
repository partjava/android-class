package com.example.toutiao.demo;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {
    private ProfileStore store;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_edit_profile);
        store = new ProfileStore(this);
        findViewById(R.id.iv_back_edit).setOnClickListener(v -> finish());
        bindText(R.id.item_username, "nickname", "昵称", 24);
        bindText(R.id.item_intro, "intro", "简介", 120);
        bindText(R.id.item_location, "location", "所在地", 40);
        bindText(R.id.item_school, "school", "学校", 40);
        bindText(R.id.item_job, "job", "职业", 40);
        findViewById(R.id.item_gender).setOnClickListener(v -> {
            String[] items = {"保密", "男", "女"};
            int cur = "男".equals(store.get("gender")) ? 1 : "女".equals(store.get("gender")) ? 2 : 0;
            new AlertDialog.Builder(this).setTitle("选择性别")
                    .setSingleChoiceItems(items, cur, (dialog, which) -> {
                        store.set("gender", items[which]);
                        showValue(R.id.item_gender, "gender");
                        dialog.dismiss();
                    }).show();
        });
        findViewById(R.id.item_birth).setOnClickListener(v -> {
            Calendar date = Calendar.getInstance();
            String[] saved = store.get("birth").split("-");
            if (saved.length == 3) date.set(Integer.parseInt(saved[0]), Integer.parseInt(saved[1]) - 1, Integer.parseInt(saved[2]));
            DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
                store.set("birth", String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, day));
                showValue(R.id.item_birth, "birth");
            }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMaxDate(System.currentTimeMillis()); picker.show();
        });
        ImageView ivAvatar = findViewById(R.id.iv_avatar_edit);
        ivAvatar.setImageResource(store.getAvatarRes());
        ((TextView)findViewById(R.id.tv_change_avatar)).setText("点击更换头像");
        final int[] avatarOptions = {
            R.drawable.image1, R.drawable.image2, R.drawable.image3,
            R.drawable.image4, R.drawable.image5, R.drawable.image6,
            R.drawable.image7, R.drawable.image8
        };
        final String[] avatarNames = {
            "航天工匠 · 高凤林", "高铁专家 · 李万君", "通信专家 · 夏立", "带电作业 · 王进",
            "地质深钻 · 朱恒银", "核能操作 · 乔素凯", "数控技师 · 陈行行", "高端维修 · 王树军"
        };
        Runnable pickAvatar = () -> new AlertDialog.Builder(this).setTitle("选择个人头像")
                .setItems(avatarNames, (d, which) -> {
                    store.setAvatarRes(avatarOptions[which]);
                    ivAvatar.setImageResource(avatarOptions[which]);
                    android.widget.Toast.makeText(this, "头像已更新，返回个人中心可见", android.widget.Toast.LENGTH_SHORT).show();
                }).show();
        ivAvatar.setOnClickListener(v -> pickAvatar.run());
        findViewById(R.id.tv_change_avatar).setOnClickListener(v -> pickAvatar.run());

        findViewById(R.id.item_bg).setOnClickListener(v -> {
            String[] bgs = {"星空深蓝", "渐变晚霞", "极简雅灰", "青春活力橙"};
            new AlertDialog.Builder(this).setTitle("选择个人主页背景")
                    .setItems(bgs, (d, which) -> {
                        store.set("profile_bg", bgs[which]);
                        showValue(R.id.item_bg, "profile_bg");
                        android.widget.Toast.makeText(this, "主页背景已设为: " + bgs[which], android.widget.Toast.LENGTH_SHORT).show();
                    }).show();
        });
        findViewById(R.id.item_avatar_frame).setOnClickListener(v -> {
            String[] badges = {"无挂件", "卓越创作者", "头条资深读者", "技术专家", "活跃打卡达人"};
            new AlertDialog.Builder(this).setTitle("选择头像挂件")
                    .setItems(badges, (d, which) -> {
                        store.set("avatar_frame", badges[which]);
                        showValue(R.id.item_avatar_frame, "avatar_frame");
                        android.widget.Toast.makeText(this, "头像挂件已佩戴: " + badges[which], android.widget.Toast.LENGTH_SHORT).show();
                    }).show();
        });

        refreshAll();
    }
    private void bindText(int rowId, String key, String title, int maxLen) {
        findViewById(rowId).setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setText(store.get(key));
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
            input.setSelection(input.getText().length());
            new AlertDialog.Builder(this).setTitle("修改" + title).setView(input)
                    .setPositiveButton("保存", (dialog, which) -> {
                        store.set(key, input.getText().toString().trim());
                        showValue(rowId, key);
                    })
                    .setNegativeButton("取消", null).show();
        });
    }
    private void showValue(int rowId, String key) {
        LinearLayout row = findViewById(rowId);
        if (row == null) return;
        TextView tv = (TextView) row.getChildAt(1);
        if (tv != null) tv.setText(store.get(key));
    }
    private void refreshAll() {
        showValue(R.id.item_username, "nickname");
        showValue(R.id.item_intro, "intro");
        showValue(R.id.item_gender, "gender");
        showValue(R.id.item_birth, "birth");
        showValue(R.id.item_location, "location");
        showValue(R.id.item_school, "school");
        showValue(R.id.item_job, "job");
        showValue(R.id.item_bg, "profile_bg");
        showValue(R.id.item_avatar_frame, "avatar_frame");
    }
}
