package com.example.toutiao.demo;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.LinkedHashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

/** Device-local course demo profile; independent of login credentials. */
public final class ProfileStore {
    static final String[] FIELDS = {"nickname", "intro", "gender", "birth", "location", "school", "job", "profile_bg", "avatar_frame", "avatar_res"};
    private final SharedPreferences prefs;
    public ProfileStore(Context context) { this(context, "profile"); }
    ProfileStore(Context context, String name) { prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE); }
    public String get(String field) { return prefs.getString(field, "nickname".equals(field) ? "凝墨" : "gender".equals(field) ? "保密" : ""); }
    public void set(String field, String value) {
        String trimmed = value == null ? "" : value.trim();
        validate(field, trimmed);
        prefs.edit().putString(field, trimmed).apply();
    }
    static void validate(String field, String value) {
        if (value == null) throw new IllegalArgumentException("资料缺少字段：" + field);
        if ("nickname".equals(field) && value.trim().isEmpty()) throw new IllegalArgumentException("昵称不能为空");
        int max = "nickname".equals(field) ? 24 : "intro".equals(field) ? 120
                : ("location".equals(field) || "school".equals(field) || "job".equals(field)) ? 40 : 120;
        if (value.length() > max) throw new IllegalArgumentException("资料过长：" + field);
        if ("gender".equals(field)) requireChoice(value, new String[]{"", "保密", "男", "女"}, "性别");
        if ("profile_bg".equals(field)) requireChoice(value, new String[]{"", "星空深蓝", "渐变晚霞", "极简雅灰", "青春活力橙"}, "背景");
        if ("avatar_frame".equals(field)) requireChoice(value, new String[]{"", "无挂件", "卓越创作者", "校园资深读者", "技术专家", "活跃打卡达人"}, "挂件");
        if ("avatar_res".equals(field)) avatarFromBackup(value);
        if ("birth".equals(field) && !value.isEmpty() && parseBirth(value) == null) throw new IllegalArgumentException("生日格式无效或晚于今天");
    }
    private static void requireChoice(String value, String[] choices, String label) {
        for (String option : choices) if (option.equals(value)) return;
        throw new IllegalArgumentException(label + "选项无效");
    }
    private static int[] avatarOptions() {
        return new int[]{R.drawable.image1, R.drawable.image2, R.drawable.image3, R.drawable.image4, R.drawable.image5, R.drawable.image6, R.drawable.image7, R.drawable.image8};
    }
    private static int avatarFromBackup(String value) {
        int[] options = avatarOptions();
        for (int i = 0; i < options.length; i++) if (("image" + (i + 1)).equals(value)) return options[i];
        try {
            int legacyId = Integer.parseInt(value);
            for (int id : options) if (legacyId == id) return id;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("头像备份无效");
    }
    private String avatarBackupName() {
        int resource = getAvatarRes(); int[] options = avatarOptions();
        for (int i = 0; i < options.length; i++) if (resource == options[i]) return "image" + (i + 1);
        return "image3";
    }
    static Date parseBirth(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA); format.setLenient(false);
            Date date = format.parse(value);
            return date != null && !date.after(new Date()) ? date : null;
        } catch (java.text.ParseException e) { return null; }
    }
    public Map<String, String> snapshot() {
        Map<String, String> data = new LinkedHashMap<>();
        for (String key : FIELDS) data.put(key, "avatar_res".equals(key) ? avatarBackupName() : get(key));
        return data;
    }
    public void restore(Map<String, String> data) {
        data = new LinkedHashMap<>(data);
        if (data.size() != FIELDS.length) throw new IllegalArgumentException("备份字段不完整");
        for (String key : FIELDS) validate(key, data.get(key));
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : FIELDS) { if ("avatar_res".equals(key)) editor.putInt(key, avatarFromBackup(data.get(key))); else editor.putString(key, data.get(key)); }
        if (!editor.commit()) throw new IllegalStateException("资料恢复写入失败");
    }
    public int getAvatarRes() {
        int resource = prefs.getInt("avatar_res", R.drawable.image3);
        for (int id : avatarOptions()) if (resource == id) return id;
        return R.drawable.image3;
    }
    public void setAvatarRes(int resId) { prefs.edit().putInt("avatar_res", resId).apply(); }
    public int getCoins() { return prefs.getInt("user_coins", 680); }
    public void addCoins(int count) { if (count > 0) prefs.edit().putInt("user_coins", getCoins() + count).apply(); }
    public boolean deductCoins(int count) {
        if (count <= 0) return true;
        int current = getCoins();
        if (current < count) return false;
        prefs.edit().putInt("user_coins", current - count).apply(); return true;
    }
}
