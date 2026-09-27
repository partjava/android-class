package com.example.toutiao.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 极简远程图片加载器。
 *
 * 给接口头条的远程封面图用：项目保持零三方依赖（不引 Glide/Picasso），
 * 这里用最朴素的方式实现同等的核心体验——内存缓存、后台线程下载、
 * 主线程回填、tag 校验防止 RecyclerView 复用错位。
 *
 * 加载失败保持占位灰底，不崩溃不影响文字内容展示。
 */
public final class RemoteImage {

    private static final Map<String, Bitmap> CACHE = new HashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RemoteImage() { }

    public static void load(ImageView iv, String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        //tag 记住这个 ImageView 当前在等哪张图，回填时校验，防止滚动复用错位
        iv.setTag(url);

        Bitmap cached;
        synchronized (CACHE) {
            cached = CACHE.get(url);
        }
        if (cached != null) {
            iv.setImageBitmap(cached);
            return;
        }

        //加载中先铺一层灰底，避免露出旧图或空白
        iv.setImageDrawable(new ColorDrawable(0x22000000));

        new Thread(() -> {
            Bitmap bmp = download(url);
            if (bmp == null) {
                return; //失败保持灰底
            }
            synchronized (CACHE) {
                CACHE.put(url, bmp);
            }
            MAIN.post(() -> {
                if (url.equals(iv.getTag())) {
                    iv.setImageBitmap(bmp);
                }
            });
        }).start();
    }

    private static Bitmap download(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) {
                return null;
            }
            InputStream in = new BufferedInputStream(conn.getInputStream());
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            return bmp;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
