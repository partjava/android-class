package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Resolution-independent weather illustrations with refined styling. */
public final class WeatherIconView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String weather;
    private final Path bolt = new Path();

    public WeatherIconView(Context context) { this(context, "晴"); }

    public WeatherIconView(Context context, String weather) {
        super(context);
        this.weather = weather == null ? "多云" : weather;
        setContentDescription(this.weather);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public String getWeather() {
        return weather;
    }

    public static String normalizeType(String type) {
        if (type == null) return "多云";
        if (type.contains("雷")) return "雷";
        if (type.contains("雪")) return "雪";
        if (type.contains("雨")) return "雨";
        if (type.contains("雾") || type.contains("霾")) return "雾";
        if (type.contains("阴")) return "阴";
        if (type.contains("云")) return "多云";
        if (type.contains("晴")) return "晴";
        return "多云";
    }

    public static boolean isRainy(String type) {
        return type != null && type.contains("雨");
    }

    public static boolean isSnowy(String type) {
        return type != null && type.contains("雪");
    }

    public static boolean isThunder(String type) {
        return type != null && (type.contains("雷") || type.contains("闪"));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;

        canvas.save();
        canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2);
        canvas.scale(size / 100f, size / 100f);

        boolean rain = isRainy(weather);
        boolean snow = isSnowy(weather);
        boolean thunder = isThunder(weather);
        boolean overcast = weather.contains("阴");
        boolean fog = weather.contains("雾") || weather.contains("霾");
        boolean hasClouds = weather.contains("云") || overcast || rain || snow || thunder;
        boolean hasSun = !overcast && !rain && !snow && !fog;

        // 1. Draw Sun (if sunny or partly cloudy)
        if (hasSun) {
            float sunCx = hasClouds ? 64f : 50f;
            float sunCy = hasClouds ? 34f : 50f;
            float sunR = hasClouds ? 18f : 23f;

            // Soft corona / outer glow
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(45, 255, 179, 0));
            canvas.drawCircle(sunCx, sunCy, sunR + 8f, paint);

            // Sun rays
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(255, 160, 0));
            int rays = 8;
            for (int i = 0; i < rays; i++) {
                double a = i * Math.PI / 4.0;
                float rStart = sunR + 5f;
                float rEnd = sunR + 11f;
                canvas.drawLine(
                    sunCx + (float) Math.cos(a) * rStart,
                    sunCy + (float) Math.sin(a) * rStart,
                    sunCx + (float) Math.cos(a) * rEnd,
                    sunCy + (float) Math.sin(a) * rEnd,
                    paint
                );
            }

            // Core sun disk
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 193, 7));
            canvas.drawCircle(sunCx, sunCy, sunR, paint);

            // Subtle sun highlight
            paint.setColor(Color.argb(120, 255, 253, 231));
            canvas.drawCircle(sunCx - sunR * 0.3f, sunCy - sunR * 0.3f, sunR * 0.45f, paint);
        }

        // 2. Draw Clouds
        if (hasClouds) {
            float baseY = hasSun ? 46f : (overcast ? 42f : 44f);

            // Back / shadow cloud layer for depth
            paint.setStyle(Paint.Style.FILL);
            int shadowColor = overcast ? Color.rgb(148, 163, 184) : Color.rgb(176, 196, 222);
            paint.setColor(shadowColor);
            canvas.drawRoundRect(16f, baseY + 4f, 86f, baseY + 30f, 13f, 13f, paint);
            canvas.drawCircle(36f, baseY + 5f, 18f, paint);
            canvas.drawCircle(58f, baseY - 1f, 23f, paint);

            // Front bright fluffy cloud layer
            int frontColor = overcast ? Color.rgb(226, 232, 240) : Color.rgb(248, 250, 252);
            paint.setColor(frontColor);
            canvas.drawRoundRect(14f, baseY + 2f, 84f, baseY + 26f, 12f, 12f, paint);
            canvas.drawCircle(34f, baseY + 3f, 17f, paint);
            canvas.drawCircle(56f, baseY - 3f, 21f, paint);
            canvas.drawCircle(70f, baseY + 7f, 13f, paint);

            // Subtle highlight on top edge
            paint.setColor(Color.argb(180, 255, 255, 255));
            canvas.drawCircle(54f, baseY - 6f, 12f, paint);
        }

        // 3. Draw Rain
        if (rain) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(37, 99, 235)); // Vibrant sky blue
            for (int i = 0; i < 4; i++) {
                float startX = 26f + i * 16f;
                float startY = 74f;
                float endX = startX - 4f;
                float endY = startY + 13f;
                canvas.drawLine(startX, startY, endX, endY, paint);
            }
        }

        // 4. Draw Snow
        if (snow) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(96, 165, 250)); // Soft icy blue
            for (int i = 0; i < 3; i++) {
                float cx = 28f + i * 22f;
                float cy = 79f;
                float r = 5.5f;
                // Vertical & Horizontal
                canvas.drawLine(cx, cy - r, cx, cy + r, paint);
                canvas.drawLine(cx - r, cy, cx + r, cy, paint);
                // Diagonals
                float d = r * 0.707f;
                canvas.drawLine(cx - d, cy - d, cx + d, cy + d, paint);
                canvas.drawLine(cx - d, cy + d, cx + d, cy - d, paint);
            }
        }

        // 5. Draw Lightning Bolt
        if (thunder) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(250, 204, 21)); // Bright lightning gold
            bolt.reset();
            bolt.moveTo(56f, 56f);
            bolt.lineTo(41f, 76f);
            bolt.lineTo(52f, 76f);
            bolt.lineTo(44f, 95f);
            bolt.lineTo(69f, 71f);
            bolt.lineTo(57f, 71f);
            bolt.close();
            canvas.drawPath(bolt, paint);

            // Subtle bolt stroke for sharpness
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f);
            paint.setColor(Color.rgb(234, 179, 8));
            canvas.drawPath(bolt, paint);
        }

        // 6. Draw Fog / Haze
        if (fog) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.5f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(148, 163, 184));
            canvas.drawLine(20f, 52f, 78f, 52f, paint);
            canvas.drawLine(14f, 63f, 84f, 63f, paint);
            canvas.drawLine(24f, 74f, 74f, 74f, paint);
        }

        canvas.restore();
    }
}
