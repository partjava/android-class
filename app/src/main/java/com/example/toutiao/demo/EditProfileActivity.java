package com.example.toutiao.demo;


        import android.os.Bundle;
        import android.view.View;
        import android.widget.ImageView;
        import android.widget.LinearLayout;
        import android.widget.TextView;
        import android.widget.Toast;
        import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {
    private ImageView ivBackEdit, ivAvatarEdit;
    private TextView tvChangeAvatar;
    private LinearLayout itemUsername, itemIntro, itemBg, itemGender, itemBirth, itemLocation, itemSchool, itemJob, itemAvatarFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        bindView();
        bindEvent();
    }

    private void bindView() {
        ivBackEdit = findViewById(R.id.iv_back_edit);
        ivAvatarEdit = findViewById(R.id.iv_avatar_edit);
        tvChangeAvatar = findViewById(R.id.tv_change_avatar);
        itemUsername = findViewById(R.id.item_username);
        itemIntro = findViewById(R.id.item_intro);
        itemBg = findViewById(R.id.item_bg);
        itemGender = findViewById(R.id.item_gender);
        itemBirth = findViewById(R.id.item_birth);
        itemLocation = findViewById(R.id.item_location);
        itemSchool = findViewById(R.id.item_school);
        itemJob = findViewById(R.id.item_job);
        itemAvatarFrame = findViewById(R.id.item_avatar_frame);
    }

    private void bindEvent() {
        ivBackEdit.setOnClickListener(v -> finish());
        tvChangeAvatar.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "打开相册选择头像", Toast.LENGTH_SHORT).show());
        ivAvatarEdit.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "打开相册选择头像", Toast.LENGTH_SHORT).show());

        itemUsername.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "修改用户名", Toast.LENGTH_SHORT).show());
        itemIntro.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "编辑简介", Toast.LENGTH_SHORT).show());
        itemBg.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "更换背景图", Toast.LENGTH_SHORT).show());
        itemGender.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "修改性别", Toast.LENGTH_SHORT).show());
        itemBirth.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "选择生日", Toast.LENGTH_SHORT).show());
        itemLocation.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "选择所在地", Toast.LENGTH_SHORT).show());
        itemSchool.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "填写学校", Toast.LENGTH_SHORT).show());
        itemJob.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "填写职业", Toast.LENGTH_SHORT).show());
        itemAvatarFrame.setOnClickListener(v -> Toast.makeText(EditProfileActivity.this, "更换头像挂件", Toast.LENGTH_SHORT).show());
    }
}
