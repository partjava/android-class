package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import java.util.List;

public final class TrendView extends View {
    private final List<WeatherReport.Day> days;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path curvePath = new Path();
    private final Path fillPath = new Path();
    private final float[] xs = new float[7], ysHigh = new float[7], ysLow = new float[7];

    public TrendView(Context context) {
        this(context, java.util.Collections.emptyList());
    }

    public TrendView(Context context, List<WeatherReport.Day> days) {
        super(context);
        this.days = days;
        StringBuilder description = new StringBuilder("七日温度趋势。");
        for (int i = 0; i < Math.min(7, days.size()); i++) {
            WeatherReport.Day day = days.get(i);
            description.append(day.date).append("最低").append(day.low).append("度最高").append(day.high).append("度。");
        }
        setContentDescription(description.toString());
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public static float calculateY(int temp, int min, int max, float top, float bottom) {
        float range = Math.max(4, max - min);
        return bottom - (temp - min) / range * (bottom - top);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = Math.min(7, days.size());
        if (n == 0) return;

        float density = getResources().getDisplayMetrics().density;
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;

        float left = 20 * density;
        float width = getWidth() - left * 2;
        float top = 32 * density;
        float bottom = getHeight() - 48 * density;

        int min = 100, max = -100;
        for (int i = 0; i < n; i++) {
            min = Math.min(min, days.get(i).low);
            max = Math.max(max, days.get(i).high);
        }

        // Background subtle grid lines
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFF1F5F9);
        paint.setStrokeWidth(1 * density);
        for (int i = 0; i < 3; i++) {
            float y = top + (bottom - top) * i / 2f;
            canvas.drawLine(left, y, getWidth() - left, y, paint);
        }

        // Compute coordinates
        for (int i = 0; i < n; i++) {
            xs[i] = left + (n == 1 ? width / 2f : width * i / (n - 1));
            ysHigh[i] = calculateY(days.get(i).high, min, max, top, bottom);
            ysLow[i] = calculateY(days.get(i).low, min, max, top, bottom);
        }

        // Draw High Temperature gradient fill
        fillPath.reset();
        fillPath.moveTo(xs[0], bottom);
        fillPath.lineTo(xs[0], ysHigh[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            fillPath.cubicTo(midX, ysHigh[i - 1], midX, ysHigh[i], xs[i], ysHigh[i]);
        }
        fillPath.lineTo(xs[n - 1], bottom);
        fillPath.close();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShader(new LinearGradient(
            0, top, 0, bottom,
            new int[]{0x40FFA726, 0x05FFA726}, null, Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);

        // Draw High Temperature curve
        curvePath.reset();
        curvePath.moveTo(xs[0], ysHigh[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            curvePath.cubicTo(midX, ysHigh[i - 1], midX, ysHigh[i], xs[i], ysHigh[i]);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.8f * density);
        paint.setColor(0xFFF59E0B); // Amber 500
        canvas.drawPath(curvePath, paint);

        // Draw Low Temperature curve
        curvePath.reset();
        curvePath.moveTo(xs[0], ysLow[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            curvePath.cubicTo(midX, ysLow[i - 1], midX, ysLow[i], xs[i], ysLow[i]);
        }
        paint.setColor(0xFF3B82F6); // Blue 500
        canvas.drawPath(curvePath, paint);

        // Draw Nodes and Labels
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < n; i++) {
            // High node (double concentric circle: outer ring + inner white)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFF59E0B);
            canvas.drawCircle(xs[i], ysHigh[i], 4.5f * density, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(xs[i], ysHigh[i], 2.5f * density, paint);

            // High temp text
            textPaint.setColor(0xFFD97706);
            textPaint.setTextSize(12 * scaledDensity);
            textPaint.setFakeBoldText(true);
            canvas.drawText(days.get(i).high + "°", xs[i], ysHigh[i] - 9 * density, textPaint);

            // Low node
            paint.setColor(0xFF3B82F6);
            canvas.drawCircle(xs[i], ysLow[i], 4.5f * density, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(xs[i], ysLow[i], 2.5f * density, paint);

            // Low temp text
            textPaint.setColor(0xFF2563EB);
            textPaint.setTextSize(12 * scaledDensity);
            textPaint.setFakeBoldText(true);
            canvas.drawText(days.get(i).low + "°", xs[i], ysLow[i] + 18 * density, textPaint);

            // Date text (e.g. 10/13)
            textPaint.setColor(0xFF64748B);
            textPaint.setTextSize(11 * scaledDensity);
            textPaint.setFakeBoldText(false);
            String date = days.get(i).date.substring(5).replace('-', '/');
            canvas.drawText(date, xs[i], getHeight() - 20 * density, textPaint);

            // Weekday text (e.g. 周二)
            textPaint.setColor(0xFF94A3B8);
            textPaint.setTextSize(10 * scaledDensity);
            String week = days.get(i).week.replace("星期", "周");
            canvas.drawText(week, xs[i], getHeight() - 6 * density, textPaint);
        }
    }
}
