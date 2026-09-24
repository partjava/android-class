package com.example.toutiao.demo;

import android.view.View;
import androidx.fragment.app.Fragment;

/** 主页面仅查找自己的视图，窗口和底部导航由 MainActivity 管理。 */
public abstract class PageFragment extends Fragment {
    protected <T extends View> T findViewById(int id) {
        return requireView().findViewById(id);
    }
}
