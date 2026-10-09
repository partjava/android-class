package com.example.toutiao.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WebActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_URL = "extra_url";

    private TextView tvTitle;
    private ImageView ivBack;
    private ImageView ivRefresh;
    private ProgressBar progressBar;
    private WebView webView;

    public static void open(Context context, String title, String url) {
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_URL, url);
        context.startActivity(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        tvTitle = findViewById(R.id.tv_web_title);
        ivBack = findViewById(R.id.iv_back);
        ivRefresh = findViewById(R.id.iv_refresh);
        progressBar = findViewById(R.id.pb_web_progress);
        webView = findViewById(R.id.wv_content);

        String initialTitle = getIntent().getStringExtra(EXTRA_TITLE);
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) url = "file:///android_asset/web/subsidy.html";
        if (!WebPagePolicy.isTrustedPage(url)) {
            loadPage(url);
            finish();
            return;
        }

        // 沉浸式状态栏与主题统一：状态栏统一设置为头条主题红，浅色文字图标
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.brand_red));
            androidx.core.view.WindowInsetsControllerCompat controller =
                    new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(false);
        }

        if (initialTitle != null && !initialTitle.isEmpty()) {
            tvTitle.setText(initialTitle);
        }

        ivBack.setOnClickListener(v -> handleBack());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
        ivRefresh.setOnClickListener(v -> {
            if (webView != null) {
                webView.reload();
            }
        });

        setupWebView();

        if (url != null && !url.isEmpty()) {
            loadPage(url);
        } else {
            loadPage("file:///android_asset/web/subsidy.html");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String url = intent.getStringExtra(EXTRA_URL);
        if (title != null && !title.isEmpty() && tvTitle != null) {
            tvTitle.setText(title);
        }
        if (url != null && !url.isEmpty() && webView != null) {
            loadPage(url);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("WebActivityJS", consoleMessage.message() + " -- Line "
                        + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar == null) return;
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && !title.isEmpty() && !title.startsWith("file://") && !title.startsWith("http")) {
                    tvTitle.setText(title);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!WebPagePolicy.isTrustedPage(url)) return null;
                try {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    // Images may load remotely, but executable content and frames stay local.
                    headers.put("Content-Security-Policy", "default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' https: data:; font-src 'self'; frame-src 'none'; object-src 'none'; connect-src 'none'; base-uri 'none'");
                    return new WebResourceResponse("text/html", "UTF-8", 200, "OK", headers,
                            getAssets().open(request.getUrl().getPath().substring("/android_asset/".length())));
                } catch (java.io.IOException failure) {
                    return new WebResourceResponse("text/plain", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
                }
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.isForMainFrame()) loadPage(request.getUrl().toString());
                return true;
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                loadPage(url);
                return true;
            }
        });
    }

    private void loadPage(String url) {
        if (WebPagePolicy.isTrustedPage(url)) {
            webView.loadUrl(url);
            return;
        }
        Uri uri = url == null ? Uri.EMPTY : Uri.parse(url);
        if ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)); }
            catch (android.content.ActivityNotFoundException e) { Toast.makeText(this, "没有可用的浏览器", Toast.LENGTH_SHORT).show(); }
        } else {
            Toast.makeText(this, "无法打开此页面", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            if (webView.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) webView.getParent()).removeView(webView);
            }
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(WebActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void vibrate(long milliseconds) {
            runOnUiThread(() -> {
                try {
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(milliseconds > 0 ? milliseconds : 50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(milliseconds > 0 ? milliseconds : 50);
                        }
                    }
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void closePage() {
            runOnUiThread(WebActivity.this::finish);
        }

        @JavascriptInterface
        public void openUrl(String title, String url) {
            runOnUiThread(() -> WebActivity.open(WebActivity.this, title, url));
        }

        @JavascriptInterface
        public void saveCoupon(int discountAmount, String title, int minSpend, String desc) {
            runOnUiThread(() -> {
                ShopStore store = new ShopStore(WebActivity.this);
                boolean added = store.addCoupon(discountAmount, title, minSpend, desc);
                vibrate(50);
                if (added) {
                    Toast.makeText(WebActivity.this, "🎉 领券成功！满 ¥" + minSpend + " 减 ¥" + discountAmount + "，已放入卡包", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(WebActivity.this, "该优惠券已在您的卡包中", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public boolean signIn() {
            return new ProfileStore(WebActivity.this).signInToday();
        }

        @JavascriptInterface
        public boolean hasSignedInToday() {
            return new ProfileStore(WebActivity.this).hasSignedInToday();
        }

        @JavascriptInterface
        public int getCoins() {
            return new ProfileStore(WebActivity.this).getCoins();
        }

        @JavascriptInterface
        public String getClaimedCoupons() {
            java.util.List<ShopStore.CouponItem> coupons = new ShopStore(WebActivity.this).getCoupons();
            org.json.JSONArray arr = new org.json.JSONArray();
            for (ShopStore.CouponItem c : coupons) {
                arr.put(c.title);
            }
            return arr.toString();
        }
    }
}
