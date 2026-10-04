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

/** Tests the draft address without changing the active service configuration. */
public final class CampusNetworkDialog {
    private CampusNetworkDialog() { }
    public static void show(Context context) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * context.getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad / 2, pad, 0);
        TextView hint = new TextView(context);
        hint.setText("模拟器连接本机服务；真机请填写运行爬虫的电脑地址。手机与电脑需在可互通的同一局域网，电脑放行5000端口。两种模式均可测试连接。");
        content.addView(hint);
        RadioGroup modes = new RadioGroup(context);
        RadioButton emulator = new RadioButton(context), device = new RadioButton(context);
        emulator.setId(android.view.View.generateViewId());device.setId(android.view.View.generateViewId());
        emulator.setText("模拟器（本机电脑）");device.setText("真机 / 自定义地址");
        modes.addView(emulator);modes.addView(device);content.addView(modes);
        EditText address = new EditText(context);
        address.setSingleLine(true);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setHint("http://192.168.1.8:5000");
        address.setText(CampusNewsStore.getServerUrl());content.addView(address);
        modes.check(CampusNetworkConfig.EMULATOR_URL.equals(CampusNewsStore.getServerUrl()) ? emulator.getId() : device.getId());
        address.setEnabled(!emulator.isChecked());
        final String[] custom = {device.isChecked() ? address.getText().toString() : ""};
        modes.setOnCheckedChangeListener((group, id) -> {
            if (id == emulator.getId()) {custom[0] = address.getText().toString();address.setText(CampusNetworkConfig.EMULATOR_URL);address.setEnabled(false);}
            else {address.setEnabled(true);address.setText(custom[0]);}
        });
        TextView status = new TextView(context);
        status.setText("测试只检查服务连接，不会更改已保存地址。");content.addView(status);
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("校园网络配置").setView(content)
                .setNeutralButton("测试连接", null).setPositiveButton("保存", null).setNegativeButton("取消", null).create();
        final int[] revision = {0};
        address.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) {revision[0]++;status.setText("地址已更改，请重新测试连接。");}
            public void afterTextChanged(Editable e) { }
        });
        dialog.setOnShowListener(ignored -> {
            Button test = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {CampusNetworkConfig.save(context, address.getText().toString());Toast.makeText(context,"已保存，返回首页刷新即可使用",Toast.LENGTH_LONG).show();dialog.dismiss();}
                catch (IllegalArgumentException e) {address.setError(e.getMessage());}
            });
            test.setOnClickListener(v -> {
                final String url;
                try {url = CampusNetworkConfig.normalize(address.getText().toString());}
                catch (IllegalArgumentException e) {address.setError(e.getMessage());return;}
                final int attempt = revision[0];test.setEnabled(false);status.setText("正在连接 " + url + " …");
                new Thread(() -> {
                    String result;HttpURLConnection conn = null;
                    try {
                        conn = (HttpURLConnection) new URL(url + "/api/health").openConnection();
                        conn.setConnectTimeout(5000);conn.setReadTimeout(5000);conn.setInstanceFollowRedirects(false);
                        int code = conn.getResponseCode();
                        if (code == 404) result = "服务可访问，但缺少检测接口。请重启更新后的 Python 爬虫。";
                        else if (code != 200) result = "连接失败：HTTP " + code;
                        else {
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            try (InputStream in = conn.getInputStream()) {byte[] buf = new byte[1024];int n;while ((n = in.read(buf)) != -1) {bytes.write(buf,0,n);if(bytes.size()>8192)throw new Exception("响应内容异常");}}
                            JSONObject body = new JSONObject(bytes.toString("UTF-8"));
                            result = "campus-news".equals(body.optString("service")) && body.optInt("code") == 200
                                    ? "连接成功：校园新闻服务可用。此测试不代表官网抓取一定成功。" : "地址可访问，但不是校园新闻服务。";
                        }
                    } catch (Exception e) {result = "连接失败：请检查服务是否启动、地址和端口、同一局域网及电脑防火墙。";}
                    finally {if(conn != null)conn.disconnect();}
                    final String message = result;
                    new Handler(Looper.getMainLooper()).post(() -> {if(!dialog.isShowing())return;test.setEnabled(true);if(revision[0] == attempt)status.setText(message);});
                }, "CampusConnectionTest").start();
            });
        });
        dialog.show();
    }
}
