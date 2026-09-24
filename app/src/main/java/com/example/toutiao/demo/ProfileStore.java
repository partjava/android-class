package com.example.toutiao.demo;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local course demo profile; independent of login credentials. */
public final class ProfileStore {
    private final SharedPreferences prefs;
    public ProfileStore(Context context) { prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE); }
    public String get(String field) {
        return prefs.getString(field, "nickname".equals(field) ? "凝墨" : "gender".equals(field) ? "保密" : "");
    }
    public void set(String field, String value) {
        String trimmed = value == null ? "" : value.trim();
        if ("nickname".equals(field) && trimmed.isEmpty()) throw new IllegalArgumentException("昵称不能为空");
        prefs.edit().putString(field, trimmed).apply();
    }
    public int getAvatarRes() {
        return prefs.getInt("avatar_res", R.drawable.image3);
    }
    public void setAvatarRes(int resId) {
        prefs.edit().putInt("avatar_res", resId).apply();
    }
}
