package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class CampusGuideActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, CampusGuideActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_guide);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.guide_fragment_container, new CampusGuideFragment())
                    .commit();
        }
    }
}
