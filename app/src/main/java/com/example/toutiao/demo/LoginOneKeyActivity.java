package com.example.toutiao.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
        setContentView(R.layout.activity_login_onekey);
        bindView();
        bindEvent();
    }

    private void bindView() {
        ivClose = findViewById(R.id.iv_close);
        btnOnekeyLogin = findViewById(R.id.btn_onekey_login);
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
            Toast.makeText(LoginOneKeyActivity.this, "抖音一键登录模拟成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginOneKeyActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        tvAgreement1.setOnClickListener(v -> Toast.makeText(LoginOneKeyActivity.this, "打开用户协议", Toast.LENGTH_SHORT).show());
        tvPrivacy1.setOnClickListener(v -> Toast.makeText(LoginOneKeyActivity.this, "打开隐私政策", Toast.LENGTH_SHORT).show());

        icPhoneLogin.setOnClickListener(v -> {
            Toast.makeText(LoginOneKeyActivity.this, "跳转到手机号登录页面", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginOneKeyActivity.this, LoginPwdActivity.class);
            startActivity(intent);
        });
        icAppleLogin.setOnClickListener(v -> Toast.makeText(LoginOneKeyActivity.this, "Apple登录", Toast.LENGTH_SHORT).show());
        icMoreLogin.setOnClickListener(v -> Toast.makeText(LoginOneKeyActivity.this, "更多登录方式", Toast.LENGTH_SHORT).show());

        tvAppSetting2.setOnClickListener(v -> Toast.makeText(LoginOneKeyActivity.this, "应用设置", Toast.LENGTH_SHORT).show());
        tvProblem2.setOnClickListener(v -> Toast.makeText(LoginOneKeyActivity.this, "遇到问题", Toast.LENGTH_SHORT).show());
    }
}
