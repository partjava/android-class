package com.example.toutiao.demo;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 晴川定制底部导航栏：
 * 突破 Material 默认 5 个 Item 上限限制，支持无缝展示 6 个主选项（首页、视频、添加、导览、商城、我的）。
 */
public class CampusBottomNavigationView extends BottomNavigationView {
    public CampusBottomNavigationView(@NonNull Context context) {
        super(context);
    }

    public CampusBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CampusBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getMaxItemCount() {
        return 8;
    }
}
