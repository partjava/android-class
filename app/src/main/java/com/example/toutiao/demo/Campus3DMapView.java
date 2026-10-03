package com.example.toutiao.demo;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 晴川三维全景互动地图组件：
 * 支持手势缩放(1.0x~3.5x)、自由拖拽漫游、双击聚焦、3D立体悬浮呼吸图钉以及平滑聚焦动画。
 */
public class Campus3DMapView extends FrameLayout {

    public interface OnLandmarkClickListener {
        void onLandmarkClick(CampusLandmark landmark);
    }

    private FrameLayout mContentContainer;
    private ImageView mIvMap;
    private FrameLayout mPinContainer;

    private float mScale = 1.0f;
    private float mTranslationX = 0f;
    private float mTranslationY = 0f;

    private float mLastTouchX;
    private float mLastTouchY;
    private boolean mIsDragging = false;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    private List<CampusLandmark> mLandmarks = new ArrayList<>();
    private final Map<String, View> mPinViewMap = new HashMap<>();
    private String mSelectedLandmarkId = null;
    private OnLandmarkClickListener mClickListener;

    private boolean mIsMarkedMode = false;

    public Campus3DMapView(@NonNull Context context) {
        this(context, null);
    }

    public Campus3DMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Campus3DMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(Color.parseColor("#1A202C"));

        mContentContainer = new FrameLayout(context);
        LayoutParams contentLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mContentContainer, contentLp);

        // 底图
        mIvMap = new ImageView(context);
        mIvMap.setImageResource(R.drawable.img_campus_3d_panorama);
        mIvMap.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mIvMap.setAdjustViewBounds(true);
        mContentContainer.addView(mIvMap, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 图钉覆盖容器
        mPinContainer = new FrameLayout(context);
        mContentContainer.addView(mPinContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 缩放手势
        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float newScale = Math.max(1.0f, Math.min(3.5f, mScale * factor));
                if (newScale != mScale) {
                    float focusX = detector.getFocusX();
                    float focusY = detector.getFocusY();
                    mTranslationX = focusX - (focusX - mTranslationX) * (newScale / mScale);
                    mTranslationY = focusY - (focusY - mTranslationY) * (newScale / mScale);
                    mScale = newScale;
                    clampTranslations();
                    applyTransform();
                }
                return true;
            }
        });

        // 单双击与滑动
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float targetScale = mScale > 1.6f ? 1.0f : 2.4f;
                zoomTo(targetScale, e.getX(), e.getY());
                return true;
            }
        });

        // 监听底图布局完成以便摆放图钉
        mIvMap.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> layoutPins());
    }

    public void setOnLandmarkClickListener(OnLandmarkClickListener listener) {
        this.mClickListener = listener;
    }

    public void setLandmarks(List<CampusLandmark> landmarks) {
        this.mLandmarks = landmarks;
        mPinContainer.removeAllViews();
        mPinViewMap.clear();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (CampusLandmark lm : landmarks) {
            View pinView = inflater.inflate(R.layout.view_campus_pin_3d, mPinContainer, false);
            TextView tvTitle = pinView.findViewById(R.id.tv_pin_title);
            tvTitle.setText(lm.name);

            // 呼吸悬浮微动效
            ObjectAnimator floatAnim = ObjectAnimator.ofFloat(pinView, "translationY", 0f, -6f, 0f);
            floatAnim.setDuration(2400 + (long) (Math.random() * 800));
            floatAnim.setRepeatCount(ValueAnimator.INFINITE);
            floatAnim.setRepeatMode(ValueAnimator.REVERSE);
            floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            floatAnim.start();

            pinView.setOnClickListener(v -> {
                selectLandmark(lm.id);
                animateToLandmark(lm);
                if (mClickListener != null) {
                    mClickListener.onLandmarkClick(lm);
                }
            });

            mPinContainer.addView(pinView);
            mPinViewMap.put(lm.id, pinView);
        }

        post(this::layoutPins);
    }

    private void layoutPins() {
        int mapW = mIvMap.getWidth();
        int mapH = mIvMap.getHeight();
        if (mapW <= 0 || mapH <= 0 || mLandmarks == null) return;

        for (CampusLandmark lm : mLandmarks) {
            View pin = mPinViewMap.get(lm.id);
            if (pin == null) continue;

            pin.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int pw = pin.getMeasuredWidth();
            int ph = pin.getMeasuredHeight();

            float px = lm.normX * mapW - pw / 2.0f;
            float py = lm.normY * mapH - ph;

            pin.setX(px);
            pin.setY(py);
        }
    }

    public void selectLandmark(String id) {
        this.mSelectedLandmarkId = id;
        for (CampusLandmark lm : mLandmarks) {
            View pin = mPinViewMap.get(lm.id);
            if (pin == null) continue;
            TextView tvTitle = pin.findViewById(R.id.tv_pin_title);
            boolean isSelected = lm.id.equals(id);

            if (isSelected) {
                tvTitle.setBackgroundResource(R.drawable.bg_pin_selected_badge);
                tvTitle.setTextColor(Color.WHITE);
                pin.animate().scaleX(1.3f).scaleY(1.3f).setDuration(200).start();
                pin.bringToFront();
            } else {
                tvTitle.setBackgroundResource(R.drawable.bg_pin_3d_badge);
                tvTitle.setTextColor(Color.parseColor("#D32F2F"));
                pin.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        }
    }

    public void filterCategory(String category) {
        for (CampusLandmark lm : mLandmarks) {
            View pin = mPinViewMap.get(lm.id);
            if (pin == null) continue;
            boolean match = "全部".equals(category) || lm.category.contains(category);
            pin.animate()
                    .alpha(match ? 1.0f : 0.22f)
                    .scaleX(match ? 1.0f : 0.8f)
                    .scaleY(match ? 1.0f : 0.8f)
                    .setDuration(250)
                    .start();
            pin.setEnabled(match);
        }
    }

    public void animateToLandmark(CampusLandmark lm) {
        if (lm == null || mIvMap.getWidth() <= 0) return;
        int viewW = getWidth();
        int viewH = getHeight();
        int mapW = mIvMap.getWidth();
        int mapH = mIvMap.getHeight();

        float targetScale = Math.max(mScale, 2.0f);
        float targetPinX = lm.normX * mapW;
        float targetPinY = lm.normY * mapH;

        float targetTransX = (viewW / 2.0f) - targetPinX * targetScale;
        float targetTransY = (viewH / 2.0f) - targetPinY * targetScale;

        // 动画过渡
        PropertyValuesHolder pScale = PropertyValuesHolder.ofFloat("scale", mScale, targetScale);
        PropertyValuesHolder pX = PropertyValuesHolder.ofFloat("transX", mTranslationX, targetTransX);
        PropertyValuesHolder pY = PropertyValuesHolder.ofFloat("transY", mTranslationY, targetTransY);

        ValueAnimator anim = ValueAnimator.ofPropertyValuesHolder(pScale, pX, pY);
        anim.setDuration(400);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            mScale = (float) animation.getAnimatedValue("scale");
            mTranslationX = (float) animation.getAnimatedValue("transX");
            mTranslationY = (float) animation.getAnimatedValue("transY");
            clampTranslations();
            applyTransform();
        });
        anim.start();
    }

    public void zoomIn() {
        zoomTo(Math.min(3.5f, mScale + 0.5f), getWidth() / 2f, getHeight() / 2f);
    }

    public void zoomOut() {
        zoomTo(Math.max(1.0f, mScale - 0.5f), getWidth() / 2f, getHeight() / 2f);
    }

    public void resetView() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        float s0 = mScale, x0 = mTranslationX, y0 = mTranslationY;
        anim.setDuration(300);
        anim.addUpdateListener(a -> {
            float f = a.getAnimatedFraction();
            mScale = s0 + (1.0f - s0) * f;
            mTranslationX = x0 + (0f - x0) * f;
            mTranslationY = y0 + (0f - y0) * f;
            applyTransform();
        });
        anim.start();
    }

    public void toggleMarkedMap() {
        mIsMarkedMode = !mIsMarkedMode;
        mIvMap.setImageResource(mIsMarkedMode ? R.drawable.img_campus_3d_panorama_marked : R.drawable.img_campus_3d_panorama);
    }

    public boolean isMarkedMode() {
        return mIsMarkedMode;
    }

    private void zoomTo(float targetScale, float focusX, float focusY) {
        float newTransX = focusX - (focusX - mTranslationX) * (targetScale / mScale);
        float newTransY = focusY - (focusY - mTranslationY) * (targetScale / mScale);

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        float s0 = mScale, x0 = mTranslationX, y0 = mTranslationY;
        anim.setDuration(280);
        anim.addUpdateListener(a -> {
            float f = a.getAnimatedFraction();
            mScale = s0 + (targetScale - s0) * f;
            mTranslationX = x0 + (newTransX - x0) * f;
            mTranslationY = y0 + (newTransY - y0) * f;
            clampTranslations();
            applyTransform();
        });
        anim.start();
    }

    private void clampTranslations() {
        int w = getWidth();
        int h = getHeight();
        float contentW = w * mScale;
        float contentH = h * mScale;

        float maxTransX = Math.max(0, (contentW - w) / 2f + w * 0.4f);
        float minTransX = -maxTransX;
        float maxTransY = Math.max(0, (contentH - h) / 2f + h * 0.4f);
        float minTransY = -maxTransY;

        mTranslationX = Math.max(minTransX, Math.min(maxTransX, mTranslationX));
        mTranslationY = Math.max(minTransY, Math.min(maxTransY, mTranslationY));
    }

    private void applyTransform() {
        mContentContainer.setPivotX(0f);
        mContentContainer.setPivotY(0f);
        mContentContainer.setScaleX(mScale);
        mContentContainer.setScaleY(mScale);
        mContentContainer.setTranslationX(mTranslationX);
        mContentContainer.setTranslationY(mTranslationY);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        if (mScaleDetector.isInProgress()) {
            mIsDragging = false;
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                mIsDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - mLastTouchX;
                float dy = event.getY() - mLastTouchY;
                if (!mIsDragging && (Math.abs(dx) > 6 || Math.abs(dy) > 6)) {
                    mIsDragging = true;
                }
                if (mIsDragging) {
                    mTranslationX += dx;
                    mTranslationY += dy;
                    clampTranslations();
                    applyTransform();
                }
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                break;
        }
        return true;
    }
}
