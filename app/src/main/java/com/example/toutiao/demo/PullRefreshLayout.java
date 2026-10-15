package com.example.toutiao.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pull down only when the content is at its top; horizontal gestures remain with children. */
public class PullRefreshLayout extends LinearLayout {
    private TextView indicator;
    private View content;
    private float startX, startY, distance;
    private boolean refreshing;
    private Runnable listener;

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        content = getChildAt(0);
        indicator = new TextView(getContext());
        indicator.setGravity(android.view.Gravity.CENTER);
        indicator.setPadding(0, dp(12), 0, dp(12));
        indicator.setTextColor(0xff246c59);
        indicator.setVisibility(GONE);
        addView(indicator, 0, new LayoutParams(-1, -2));
    }

    public void setOnRefreshListener(Runnable listener) { this.listener = listener; }
    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
        indicator.setText("正在刷新商品…");
        indicator.setVisibility(refreshing ? VISIBLE : GONE);
    }
    public boolean isRefreshing() { return refreshing; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            startX = event.getX(); startY = event.getY(); distance = 0;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !refreshing && isEnabled()
                && !content.canScrollVertically(-1)) {
            float dy = event.getY() - startY;
            return dy > ViewConfiguration.get(getContext()).getScaledTouchSlop()
                    && dy > Math.abs(event.getX() - startX);
        }
        return false;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                distance = Math.max(0, event.getY() - startY);
                indicator.setVisibility(VISIBLE);
                indicator.setText(distance >= dp(90) ? "松开刷新" : "下拉刷新");
                return true;
            case MotionEvent.ACTION_UP:
                if (distance >= dp(90) && listener != null && !refreshing) {
                    setRefreshing(true);
                    listener.run();
                } else setRefreshing(false);
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (!refreshing) setRefreshing(false);
                return true;
            default: return true;
        }
    }
}
