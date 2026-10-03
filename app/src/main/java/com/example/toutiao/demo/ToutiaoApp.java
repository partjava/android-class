package com.example.toutiao.demo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ToutiaoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        applyNightMode(this);
    }

    public static void applyNightMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences("settings", MODE_PRIVATE);
        int mode = sp.getInt("dark_mode_idx", 0);
        if (mode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (mode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
