package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * 今日头条沉浸式发布弹窗选择器。
 * 支持「发微头条」、「写长文章」、「发小视频」、「提问题」四大创作形态。
 */
public class PublishDialog {
    public static void show(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context).create();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_publish_chooser, null);
        dialog.setView(view);

        view.findViewById(R.id.ll_entry_micro).setOnClickListener(v -> {
            dialog.dismiss();
            PublishActivity.start(context, PublishActivity.MODE_MICRO);
        });

        view.findViewById(R.id.ll_entry_article).setOnClickListener(v -> {
            dialog.dismiss();
            PublishActivity.start(context, PublishActivity.MODE_ARTICLE);
        });

        view.findViewById(R.id.ll_entry_video).setOnClickListener(v -> {
            dialog.dismiss();
            PublishActivity.start(context, PublishActivity.MODE_VIDEO);
        });

        view.findViewById(R.id.ll_entry_qa).setOnClickListener(v -> {
            dialog.dismiss();
            PublishActivity.start(context, PublishActivity.MODE_QA);
        });

        view.findViewById(R.id.iv_close_chooser).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
