package com.example.toutiao.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.lang.reflect.Field;

/**
 * 晴川定制底部导航栏：
 * 突破 Material 默认 5 个 Item 上限限制，支持无缝展示 6 个主选项（首页、视频、添加、导览、商城、我的）。
 */
public class CampusBottomNavigationView extends BottomNavigationView {
    public CampusBottomNavigationView(@NonNull Context context) {
        super(context);
        expandCapacity();
    }

    public CampusBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        expandCapacity();
    }

    public CampusBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        expandCapacity();
    }

    @Override
    public int getMaxItemCount() {
        return 10;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        expandCapacity();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void expandCapacity() {
        try {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child instanceof BottomNavigationMenuView) {
                    Field f = BottomNavigationMenuView.class.getDeclaredField("tempChildWidths");
                    f.setAccessible(true);
                    int[] arr = (int[]) f.get(child);
                    if (arr == null || arr.length < 12) {
                        f.set(child, new int[16]);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
