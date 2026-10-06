package com.example.toutiao.demo;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/** Offline 3D mode. Only packaged assets are loaded; no exposed JavaScript bridge. */
public class CampusSceneActivity extends AppCompatActivity {
    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_scene);
        findViewById(R.id.scene_back).setOnClickListener(v -> finish());
        web = findViewById(R.id.campus_scene_web);
        WebView.setWebContentsDebuggingEnabled(true);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setBlockNetworkLoads(true);
        web.setWebChromeClient(new android.webkit.WebChromeClient());
        web.addJavascriptInterface(new CampusNativeBridge(), "CampusNativeBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }
        });
        web.loadUrl("file:///android_asset/campus3d/index.html");
    }

    public class CampusNativeBridge {
        @android.webkit.JavascriptInterface
        public void saveConfig(String key, String value) {
            getSharedPreferences("campus3d_custom", MODE_PRIVATE)
                    .edit().putString(key, value).apply();
        }

        @android.webkit.JavascriptInterface
        public String loadConfig(String key) {
            return getSharedPreferences("campus3d_custom", MODE_PRIVATE)
                    .getString(key, null);
        }

        @android.webkit.JavascriptInterface
        public void copyToClipboard(String text) {
            runOnUiThread(() -> {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("campus_export", text));
                    android.widget.Toast.makeText(CampusSceneActivity.this, "已复制到剪贴板！", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean handleUrl(Uri uri) {
        if ("campus".equals(uri.getScheme()) && "detail".equals(uri.getHost())) {
            String id = uri.getLastPathSegment();
            if (id != null && id.matches("[a-z0-9_]+")) showDetail(id);
        }
        // Scene links never navigate the WebView, including external URLs.
        return true;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void showDetail(String id) {
        String lookup = id.startsWith("dorm_") || "small_dorm".equals(id) ? "dorm_cluster" : id;
        CampusLandmark landmark = null;
        for (CampusLandmark item : CampusLandmark.getAllLandmarks()) {
            if (item.id.equals(lookup)) { landmark = item; break; }
        }
        if (landmark == null) return;
        String name = landmark.name;
        String floors = landmark.totalFloors;
        if (id.startsWith("dorm_")) { name = id.substring(5) + "号宿舍"; floors = "共6层"; }
        if ("small_dorm".equals(id)) { name = "小宿舍"; floors = "共3层（图片为宿舍区参考）"; }
        if ("library".equals(id)) { name = "图书馆"; floors = "共5层：地下1层、地上4层"; }
        if ("theater_market".equals(id)) floors = "共2层：1楼超市，2楼小剧场";
        if ("art_museum".equals(id)) { name = "美术馆"; floors = "楼层数待确认，模型高度暂按照片估算"; }
        if ("computer_center".equals(id)) { name = "计算机实验室"; floors = "楼层数待确认，模型高度暂按照片估算"; }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(18),dp(20),dp(24));
        TextView title = new TextView(this);
        title.setText(name + "\n" + floors); title.setTextSize(19); title.setTextColor(0xff215e48);
        content.addView(title);
        ImageView image = new ImageView(this);
        image.setImageResource(landmark.resImage); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(name + "实景参考");
        content.addView(image,new LinearLayout.LayoutParams(-1,dp(220)));
        TextView note = new TextView(this);
        note.setText("实景图片为建筑或所在区域参考。三维模型依据截图近似重建，未制作入口与室内布局。");
        note.setTextSize(13); content.addView(note);
        TextView close = new TextView(this);close.setText("返回三维地图");close.setTextSize(16);close.setPadding(0,dp(18),0,dp(8));
        close.setOnClickListener(v -> dialog.dismiss());content.addView(close);
        dialog.setContentView(content);dialog.show();
    }

    @Override protected void onPause() {
        if (web != null) { web.evaluateJavascript("window.CampusScene && CampusScene.pause(true)",null); web.onPause(); }
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (web != null) { web.onResume(); web.evaluateJavascript("window.CampusScene && CampusScene.pause(false)",null); }
    }
    @Override protected void onDestroy() {
        if (web != null) { ((android.view.ViewGroup)web.getParent()).removeView(web);web.destroy();web=null; }
        super.onDestroy();
    }
}
