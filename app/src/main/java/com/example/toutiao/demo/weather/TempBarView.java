package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Horizontal temperature range indicator bar, visualizing day's min/max
 * within the overall 15-day range.
 */
public final class TempBarView extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF barRect = new RectF();

    private int globalMin = 0;
    private int globalMax = 30;
    private int dayLow = 10;
    private int dayHigh = 20;

    public TempBarView(Context context) {
        super(context);
        trackPaint.setColor(0xFFE2E8F0); // Subtle slate track
        trackPaint.setStyle(Paint.Style.FILL);
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setRange(int globalMin, int globalMax, int dayLow, int dayHigh) {
        this.globalMin = globalMin;
        this.globalMax = globalMax;
        this.dayLow = dayLow;
        this.dayHigh = dayHigh;
        invalidate();
    }

    public static float[] calculateBarBounds(int globalMin, int globalMax, int dayLow, int dayHigh, float totalWidth, float minWidth) {
        if (globalMax <= globalMin || totalWidth <= 0) {
            return new float[]{0f, totalWidth};
        }
        float range = (float) (globalMax - globalMin);
        float left = totalWidth * (Math.max(globalMin, dayLow) - globalMin) / range;
        float right = totalWidth * (Math.min(globalMax, dayHigh) - globalMin) / range;

        if (right - left < minWidth) {
            float mid = (left + right) / 2f;
            left = Math.max(0f, mid - minWidth / 2f);
            right = Math.min(totalWidth, left + minWidth);
            if (right - left < minWidth) {
                left = Math.max(0f, right - minWidth);
            }
        }
        return new float[]{left, right};
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float radius = height / 2f;

        // 1. Draw background track
        trackRect.set(0f, 0f, width, height);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        // 2. Draw active range bar with gradient
        float minBarWidth = 8f * density;
        float[] bounds = calculateBarBounds(globalMin, globalMax, dayLow, dayHigh, width, minBarWidth);
        barRect.set(bounds[0], 0f, bounds[1], height);

        // Linear gradient from cool blue to warm amber across the entire component width
        barPaint.setShader(new LinearGradient(
            0f, 0f, width, 0f,
            new int[]{0xFF3B82F6, 0xFF10B981, 0xFFF59E0B},
            new float[]{0f, 0.5f, 1f},
            Shader.TileMode.CLAMP
        ));

        canvas.drawRoundRect(barRect, radius, radius, barPaint);
    }
}
