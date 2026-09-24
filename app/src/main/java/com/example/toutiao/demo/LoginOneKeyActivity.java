package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginOneKeyActivity extends AppCompatActivity {
    private ImageView ivClose, icPhoneLogin, icAppleLogin, icMoreLogin;
    private Button btnOnekeyLogin;
    private CheckBox cbAgreeOnekey;
    private TextView tvAgreement1, tvPrivacy1, tvAppSetting2, tvProblem2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSharedPreferences("session", MODE_PRIVATE).getBoolean("logged_in", false)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login_onekey);
        bindView();
        bindEvent();
    }

    private void bindView() {
        ivClose = findViewById(R.id.iv_close);
        btnOnekeyLogin = findViewById(R.id.btn_onekey_login);
        btnOnekeyLogin.setText("进入本地体验");
        cbAgreeOnekey = findViewById(R.id.cb_agree_onekey);
        tvAgreement1 = findViewById(R.id.tv_agreement1);
        tvPrivacy1 = findViewById(R.id.tv_privacy1);
        icPhoneLogin = findViewById(R.id.ic_phone_login);
        icAppleLogin = findViewById(R.id.ic_apple_login);
        icMoreLogin = findViewById(R.id.ic_more_login);
        tvAppSetting2 = findViewById(R.id.tv_app_setting2);
        tvProblem2 = findViewById(R.id.tv_problem2);
    }

    private void bindEvent() {
        ivClose.setOnClickListener(v -> finish());

        btnOnekeyLogin.setOnClickListener(v -> {
            if (!cbAgreeOnekey.isChecked()) {
                Toast.makeText(LoginOneKeyActivity.this, "请勾选同意协议", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("session", MODE_PRIVATE).edit().putBoolean("logged_in", true).apply();
            Toast.makeText(LoginOneKeyActivity.this, "已进入本地课程体验", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginOneKeyActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        tvAgreement1.setOnClickListener(v -> LegalDialogs.showAgreement(this));
        tvPrivacy1.setOnClickListener(v -> LegalDialogs.showPrivacy(this));

        icPhoneLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginOneKeyActivity.this, LoginPwdActivity.class);
            startActivity(intent);
        });

        icAppleLogin.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Apple ID 模拟登录")
                    .setMessage("检测到 Apple 登录凭证请求。\n是否通过 Apple 账户“Android Demo User”直接授权登录？")
                    .setPositiveButton("授权并登录", (d, w) -> {
                        getSharedPreferences("session", MODE_PRIVATE).edit().putBoolean("logged_in", true).apply();
                        Toast.makeText(this, "Apple ID 授权登录成功", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        icMoreLogin.setOnClickListener(v -> {
            String[] ways = {"微信一键授权", "QQ快捷登录", "微博账号登录", "访客临时体验"};
            new AlertDialog.Builder(this)
                    .setTitle("选择其他登录方式")
                    .setItems(ways, (d, which) -> {
                        getSharedPreferences("session", MODE_PRIVATE).edit().putBoolean("logged_in", true).apply();
                        Toast.makeText(this, "使用【" + ways[which] + "】登录成功", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        tvAppSetting2.setOnClickListener(v -> {
            Intent intent = new Intent(LoginOneKeyActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        tvProblem2.setOnClickListener(v -> LegalDialogs.showHelp(this));
    }
}
