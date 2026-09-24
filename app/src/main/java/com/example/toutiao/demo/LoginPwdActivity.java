package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginPwdActivity extends AppCompatActivity {
    private ImageView ivBack;
    private EditText etAccount, etPwd;
    private Button btnLogin;
    private CheckBox cbAgree;
    private TextView tvForgetPwd, tvUserAgreement, tvPrivacy, tvAppSetting, tvProblem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_pwd);
        bindView();
        bindEvent();
    }

    private void bindView() {
        ivBack = findViewById(R.id.iv_back);
        etAccount = findViewById(R.id.et_account);
        etPwd = findViewById(R.id.et_pwd);
        btnLogin = findViewById(R.id.btn_login);
        cbAgree = findViewById(R.id.cb_agree);
        tvForgetPwd = findViewById(R.id.tv_forget_pwd);
        tvUserAgreement = findViewById(R.id.tv_user_agreement);
        tvPrivacy = findViewById(R.id.tv_privacy);
        tvAppSetting = findViewById(R.id.tv_app_setting);
        tvProblem = findViewById(R.id.tv_problem);
    }

    private void bindEvent() {
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        btnLogin.setOnClickListener(v -> {
            String account = etAccount.getText().toString().trim();
            String pwd = etPwd.getText().toString().trim();

            if (TextUtils.isEmpty(account)) {
                Toast.makeText(LoginPwdActivity.this, "请输入手机号或账号", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(pwd)) {
                Toast.makeText(LoginPwdActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!cbAgree.isChecked()) {
                Toast.makeText(LoginPwdActivity.this, "请勾选同意《用户协议》和《隐私政策》", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("session", MODE_PRIVATE).edit().putBoolean("logged_in", true).apply();
            Toast.makeText(LoginPwdActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginPwdActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 找回密码
        tvForgetPwd.setOnClickListener(v -> {
            EditText etNewPwd = new EditText(this);
            etNewPwd.setHint("请输入新密码");
            new AlertDialog.Builder(this)
                    .setTitle("找回 / 重置密码")
                    .setMessage("验证码已模拟发送至绑定的安全手机。请直接输入新密码：")
                    .setView(etNewPwd)
                    .setPositiveButton("重置密码", (d, w) -> {
                        String newP = etNewPwd.getText().toString().trim();
                        if (!newP.isEmpty()) {
                            etPwd.setText(newP);
                            Toast.makeText(this, "密码重置成功，已填入密码框", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 用户协议
        tvUserAgreement.setOnClickListener(v -> LegalDialogs.showAgreement(this));
        
        // 隐私政策
        tvPrivacy.setOnClickListener(v -> LegalDialogs.showPrivacy(this));
        
        // 应用设置
        tvAppSetting.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPwdActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 遇到问题
        tvProblem.setOnClickListener(v -> LegalDialogs.showHelp(this));
    }
}
