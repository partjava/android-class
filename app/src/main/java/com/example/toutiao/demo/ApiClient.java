package com.example.toutiao.demo;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 晴川校园统一 RESTful 网络客户端。
 * 原生 HttpURLConnection 实现，严格支持 GET 与 POST 双协议请求，
 * 异步执行并通过 Handler 自动切回主线程回调。零外部第三方库依赖。
 */
public final class ApiClient {

    private static final int TIMEOUT_MS = 6000;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final ExecutorService CART_EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    private ApiClient() { }

    /**
     * 发起 HTTP GET 请求
     * @param urlString 完整请求地址
     * @param callback  主线程异步回调
     */
    public static void get(final String urlString, final Callback callback) {
        EXECUTOR.execute(() -> executeRequest("GET", urlString, null, callback));
    }

    public static void getWithReadTimeout(final String urlString, final int readTimeoutMs, final Callback callback) {
        EXECUTOR.execute(() -> executeRequest("GET", urlString, null, callback, readTimeoutMs));
    }

    /**
     * 发起 HTTP POST 请求
     * @param urlString 完整请求地址
     * @param jsonBody  JSON 请求体字符串
     * @param callback  主线程异步回调
     */
    public static void post(final String urlString, final String jsonBody, final Callback callback) {
        EXECUTOR.execute(() -> executeRequest("POST", urlString, jsonBody, callback));
    }

    public static void postCart(final String urlString, final String jsonBody) {
        CART_EXECUTOR.execute(() -> executeRequest("POST", urlString, jsonBody, null));
    }

    private static void executeRequest(String method, String urlString, String jsonBody, Callback callback) {
        executeRequest(method, urlString, jsonBody, callback, TIMEOUT_MS);
    }

    private static void executeRequest(String method, String urlString, String jsonBody, Callback callback, int readTimeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(readTimeoutMs);
            conn.setUseCaches(false);
            conn.setRequestProperty("Accept", "application/json");

            if ("POST".equalsIgnoreCase(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                if (jsonBody != null && !jsonBody.isEmpty()) {
                    byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                    conn.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bytes);
                        os.flush();
                    }
                }
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            String responseStr = "";
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                responseStr = sb.toString().trim();
            }

            if (code >= 200 && code < 300) {
                JSONObject json;
                try {
                    json = new JSONObject(responseStr);
                } catch (JSONException je) {
                    json = new JSONObject();
                    json.put("code", code);
                    json.put("raw", responseStr);
                }
                final JSONObject finalJson = json;
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onSuccess(finalJson);
                });
            } else {
                final String errorMsg = "HTTP " + code + (responseStr.isEmpty() ? "" : ": " + responseStr);
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onError(errorMsg);
                });
            }
        } catch (final Exception e) {
            final String errorMsg = e.getMessage() == null ? "网络连接异常" : e.getMessage();
            MAIN_HANDLER.post(() -> {
                if (callback != null) callback.onError(errorMsg);
            });
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
