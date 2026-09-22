package com.example.toutiao.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MineActivity extends AppCompatActivity {
    private ImageView ivSetting, ivAvatar;
    private TextView tvApplyAuth;
    private LinearLayout llMsgPrivate, llHistory, llCreate, llBook, llShop, llCollect, llService, llRefund;
    private Button btnGoComment;
    //新增底部导航控件
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine);
        bindView();
        bindEvent();
    }

    private void bindView() {
        ivSetting = findViewById(R.id.iv_setting);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvApplyAuth = findViewById(R.id.tv_apply_auth);
        llMsgPrivate = findViewById(R.id.ll_msg_private);
        llHistory = findViewById(R.id.ll_history);
        llCreate = findViewById(R.id.ll_create);
        llBook = findViewById(R.id.ll_book);
        llShop = findViewById(R.id.ll_shop);
        llCollect = findViewById(R.id.ll_collect);
        llService = findViewById(R.id.ll_service);
        llRefund = findViewById(R.id.ll_refund);
        btnGoComment = findViewById(R.id.btn_go_comment);
        //绑定底部导航
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void bindEvent() {
        // 设置按钮 跳设置页
        ivSetting.setOnClickListener(v -> {
            Intent intent = new Intent(MineActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        //头像点击，跳编辑资料
        ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(MineActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
        tvApplyAuth.setOnClickListener(v -> Toast.makeText(MineActivity.this, "申请认证", Toast.LENGTH_SHORT).show());

        //我的功能各个按钮
        llMsgPrivate.setOnClickListener(v -> Toast.makeText(MineActivity.this, "消息私信", Toast.LENGTH_SHORT).show());
        llHistory.setOnClickListener(v -> Toast.makeText(MineActivity.this, "浏览历史", Toast.LENGTH_SHORT).show());
        llCreate.setOnClickListener(v -> Toast.makeText(MineActivity.this, "创作中心", Toast.LENGTH_SHORT).show());
        llBook.setOnClickListener(v -> Toast.makeText(MineActivity.this, "书架", Toast.LENGTH_SHORT).show());
        llShop.setOnClickListener(v -> Toast.makeText(MineActivity.this, "购物/订单", Toast.LENGTH_SHORT).show());
        llCollect.setOnClickListener(v -> Toast.makeText(MineActivity.this, "收藏", Toast.LENGTH_SHORT).show());
        llService.setOnClickListener(v -> Toast.makeText(MineActivity.this, "客服中心", Toast.LENGTH_SHORT).show());
        llRefund.setOnClickListener(v -> Toast.makeText(MineActivity.this, "退款/售后", Toast.LENGTH_SHORT).show());

        btnGoComment.setOnClickListener(v -> Toast.makeText(MineActivity.this, "去评论", Toast.LENGTH_SHORT).show());

        //=========底部导航点击事件【已修复首页跳转】=========
        bottomNav.setSelectedItemId(R.id.nav_mine);
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if(itemId == R.id.nav_home){
                    //从我的页面回到首页，复用栈里已有的 HomeActivity
                    Intent intent = new Intent(MineActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    //CLEAR_TOP 已经会把 MineActivity 从栈里清掉，
                    //这里的 finish() 是兜底：万一栈里没有 HomeActivity，不至于让"我的"留在下面
                    finish();
                }else if(itemId == R.id.nav_video){
                    Toast.makeText(MineActivity.this,"点击视频",Toast.LENGTH_SHORT).show();
                }else if(itemId == R.id.nav_add){
                    Toast.makeText(MineActivity.this,"点击发布",Toast.LENGTH_SHORT).show();
                }else if(itemId == R.id.nav_shop){
                    //跳转到商城页
                    Intent intent = new Intent(MineActivity.this, ShopActivity.class);
                    startActivity(intent);
                }else if(itemId == R.id.nav_mine){
                    Toast.makeText(MineActivity.this,"当前在我的页面",Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }
}
