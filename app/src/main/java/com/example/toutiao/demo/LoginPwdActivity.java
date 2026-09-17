package com.example.toutiao.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
        // 返回
        ivBack.setOnClickListener(v -> finish());

        // 立即登录
        btnLogin.setOnClickListener(v -> {
            if (!cbAgree.isChecked()) {
                Toast.makeText(LoginPwdActivity.this, "请勾选同意用户协议与隐私政策", Toast.LENGTH_SHORT).show();
                return;
            }
            String account = etAccount.getText().toString().trim();
            String pwd = etPwd.getText().toString().trim();
            if(account.isEmpty()){
                Toast.makeText(LoginPwdActivity.this, "请输入手机号/邮箱", Toast.LENGTH_SHORT).show();
                return;
            }
            if(pwd.isEmpty()){
                Toast.makeText(LoginPwdActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
                return;
            }
            //模拟登录成功，进入首页（和一键登录保持一致）
            Toast.makeText(LoginPwdActivity.this, "账号密码登录模拟成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginPwdActivity.this, HomeActivity.class);
            //清掉登录页所在的整个任务栈，否则登录后按返回键又退回登录界面
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        //找回密码
        tvForgetPwd.setOnClickListener(v -> Toast.makeText(LoginPwdActivity.this, "打开找回密码页面", Toast.LENGTH_SHORT).show());
        //用户协议
        tvUserAgreement.setOnClickListener(v -> Toast.makeText(LoginPwdActivity.this, "打开用户协议", Toast.LENGTH_SHORT).show());
        //隐私政策
        tvPrivacy.setOnClickListener(v -> Toast.makeText(LoginPwdActivity.this, "打开隐私政策", Toast.LENGTH_SHORT).show());
        //应用设置
        tvAppSetting.setOnClickListener(v -> Toast.makeText(LoginPwdActivity.this, "打开应用设置", Toast.LENGTH_SHORT).show());
        //遇到问题
        tvProblem.setOnClickListener(v -> Toast.makeText(LoginPwdActivity.this, "打开问题反馈", Toast.LENGTH_SHORT).show());
    }
}
