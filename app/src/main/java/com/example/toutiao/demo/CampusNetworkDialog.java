package com.example.toutiao.demo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Tests the draft address without changing the active service configuration, supporting FastAPI and Flask engine switching. */
public final class CampusNetworkDialog {
    private CampusNetworkDialog() { }

    public static void show(Context context) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * context.getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad / 2, pad, 0);

        TextView hint = new TextView(context);
        hint.setText("可自由切换 FastAPI (8000) 或 Flask (5000) 后端服务。模拟器连接本机电脑；真机请填写电脑局域网IP并放行对应端口。");
        content.addView(hint);

        // 1. 后端服务引擎切换 (FastAPI / Flask)
        TextView engineTitle = new TextView(context);
        engineTitle.setText("\n【后端服务引擎切换】");
        engineTitle.getPaint().setFakeBoldText(true);
        content.addView(engineTitle);

        RadioGroup engineGroup = new RadioGroup(context);
        RadioButton rbFastapi = new RadioButton(context), rbFlask = new RadioButton(context);
        rbFastapi.setId(android.view.View.generateViewId());
        rbFlask.setId(android.view.View.generateViewId());
        rbFastapi.setText("FastAPI 引擎（8000 端口 · 动态 API 与交互文档）");
        rbFlask.setText("Flask 引擎（5000 端口 · 经典稳定版）");
        engineGroup.addView(rbFastapi);
        engineGroup.addView(rbFlask);
        content.addView(engineGroup);

        String activeEngine = CampusNetworkConfig.getEngine(context);
        engineGroup.check(CampusNetworkConfig.ENGINE_FLASK.equals(activeEngine) ? rbFlask.getId() : rbFastapi.getId());

        // 2. 运行环境模式切换 (模拟器 / 真机)
        TextView modeTitle = new TextView(context);
        modeTitle.setText("\n【运行环境模式】");
        modeTitle.getPaint().setFakeBoldText(true);
        content.addView(modeTitle);

        RadioGroup modes = new RadioGroup(context);
        RadioButton emulator = new RadioButton(context), device = new RadioButton(context);
        emulator.setId(android.view.View.generateViewId());
        device.setId(android.view.View.generateViewId());
        emulator.setText("模拟器（本机电脑）");
        device.setText("真机 / 局域网自定义地址");
        modes.addView(emulator);
        modes.addView(device);
        content.addView(modes);

        EditText address = new EditText(context);
        address.setSingleLine(true);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setHint("http://192.168.1.8:8000");
        address.setText(CampusNewsStore.getServerUrl());
        content.addView(address);

        boolean isEmulator = CampusNetworkConfig.FLASK_EMULATOR_URL.equals(CampusNewsStore.getServerUrl())
                || CampusNetworkConfig.FASTAPI_EMULATOR_URL.equals(CampusNewsStore.getServerUrl());
        modes.check(isEmulator ? emulator.getId() : device.getId());
        address.setEnabled(!emulator.isChecked());

        final String[] custom = {device.isChecked() ? address.getText().toString() : ""};

        // 引擎切换与地址联动逻辑
        engineGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isFastapi = (checkedId == rbFastapi.getId());
            int targetPort = isFastapi ? 8000 : 5000;
            if (emulator.isChecked()) {
                address.setText(isFastapi ? CampusNetworkConfig.FASTAPI_EMULATOR_URL : CampusNetworkConfig.FLASK_EMULATOR_URL);
            } else {
                String cur = address.getText().toString();
                if (cur.contains(":5000")) {
                    address.setText(cur.replace(":5000", ":" + targetPort));
                } else if (cur.contains(":8000")) {
                    address.setText(cur.replace(":8000", ":" + targetPort));
                }
            }
        });

        modes.setOnCheckedChangeListener((group, id) -> {
            boolean isFastapi = (engineGroup.getCheckedRadioButtonId() == rbFastapi.getId());
            if (id == emulator.getId()) {
                custom[0] = address.getText().toString();
                address.setText(isFastapi ? CampusNetworkConfig.FASTAPI_EMULATOR_URL : CampusNetworkConfig.FLASK_EMULATOR_URL);
                address.setEnabled(false);
            } else {
                address.setEnabled(true);
                address.setText(custom[0]);
            }
        });

        TextView status = new TextView(context);
        status.setText("\n测试只检查服务连接，不会更改已保存地址。");
        content.addView(status);

        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("校园网络与后端配置").setView(content)
                .setNeutralButton("测试连接", null).setPositiveButton("保存", null).setNegativeButton("取消", null).create();

        final int[] revision = {0};
        address.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                revision[0]++;
                status.setText("\n地址已更改，请重新测试连接。");
            }
            public void afterTextChanged(Editable e) { }
        });

        dialog.setOnShowListener(ignored -> {
            Button test = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    String selectedEngine = (engineGroup.getCheckedRadioButtonId() == rbFastapi.getId())
                            ? CampusNetworkConfig.ENGINE_FASTAPI : CampusNetworkConfig.ENGINE_FLASK;
                    CampusNetworkConfig.save(context, address.getText().toString(), selectedEngine);
                    Toast.makeText(context, "已保存后端配置 (" + (selectedEngine.equals("fastapi") ? "FastAPI 8000" : "Flask 5000") + ")，返回首页即可生效", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    address.setError(e.getMessage());
                }
            });

            test.setOnClickListener(v -> {
                final String url;
                try {
                    url = CampusNetworkConfig.normalize(address.getText().toString());
                } catch (IllegalArgumentException e) {
                    address.setError(e.getMessage());
                    return;
                }
                final int attempt = revision[0];
                test.setEnabled(false);
                status.setText("\n正在连接 " + url + " …");
                new Thread(() -> {
                    String result;
                    HttpURLConnection conn = null;
                    try {
                        conn = (HttpURLConnection) new URL(url + "/api/health").openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        conn.setInstanceFollowRedirects(false);
                        int code = conn.getResponseCode();
                        if (code == 404) {
                            result = "\n服务可访问，但缺少检测接口。请检查服务是否为校园服务端。";
                        } else if (code != 200) {
                            result = "\n连接失败：HTTP " + code;
                        } else {
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            try (InputStream in = conn.getInputStream()) {
                                byte[] buf = new byte[1024];
                                int n;
                                while ((n = in.read(buf)) != -1) {
                                    bytes.write(buf, 0, n);
                                    if (bytes.size() > 8192) throw new Exception("响应内容异常");
                                }
                            }
                            JSONObject body = new JSONObject(bytes.toString("UTF-8"));
                            if ("campus-news".equals(body.optString("service")) && body.optInt("code") == 200) {
                                String engine = body.optString("engine", "flask");
                                int port = body.optInt("port", url.contains(":8000") ? 8000 : 5000);
                                result = "\n连接成功！当前在线后端：" + ("fastapi".equalsIgnoreCase(engine) ? "FastAPI 现代异步后端" : "Flask 经典服务")
                                        + " (端口 " + port + ")。";
                            } else {
                                result = "\n地址可访问，但返回内容不是预期的校园新闻服务。";
                            }
                        }
                    } catch (Exception e) {
                        result = "\n连接失败：请确认后端服务已启动、端口正确、处于同一局域网并放行防火墙。";
                    } finally {
                        if (conn != null) conn.disconnect();
                    }
                    final String message = result;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!dialog.isShowing()) return;
                        test.setEnabled(true);
                        if (revision[0] == attempt) status.setText(message);
                    });
                }, "CampusConnectionTest").start();
            });
        });
        dialog.show();
    }
}
