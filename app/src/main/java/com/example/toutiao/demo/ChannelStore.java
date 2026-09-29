package com.example.toutiao.demo;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 频道数据管理与本地持久化中心 (ChannelStore)。
 *
 * 管理首页顶部可横向滑动的频道列表以及「频道管理」弹窗中的【我的频道】与【更多频道】。
 * 包含武汉晴川学院官网（qcuwh.cn）全部核心栏目，支持自由增删与本地持久化。
 */
public final class ChannelStore {
    private static final String PREF_NAME = "toutiao_channels";
    private static final String KEY_MY_CHANNELS = "key_my_channels";
    private static final String KEY_MORE_CHANNELS = "key_more_channels";

    private ChannelStore() { }

    // =========================================================================
    // 【开发者扩展引导】
    // 若后续想添加更多频道种类，只需在下方列表中继续追加即可：
    // list.add(new Channel("unique_id", "频道名称", false));
    // =========================================================================

    /**
     * 默认【我的频道】初始列表 (已默认加载学校官网的核心多标题)
     */
    public static List<Channel> getDefaultMyChannels() {
        List<Channel> list = new ArrayList<>();
        // 固定频道 (isFixed=true，不可删除)
        list.add(new Channel("recommend", "推荐", true));
        list.add(new Channel("campus", "校园要闻", true));

        // 武汉晴川学院官网核心多标题 (支持编辑与排序)
        list.add(new Channel("survey", "学校概况", false));
        list.add(new Channel("org", "机构设置", false));
        list.add(new Channel("talent", "人才培养", false));
        list.add(new Channel("faculty", "师资队伍", false));
        list.add(new Channel("research", "教学科研", false));
        list.add(new Channel("admissions", "招生就业", false));
        return list;
    }

    /**
     * 默认【更多频道 / 推荐添加】初始列表 (点击即可添加至我的频道)
     */
    public static List<Channel> getDefaultMoreChannels() {
        List<Channel> list = new ArrayList<>();
        // 晴川学院官网后续栏目
        list.add(new Channel("party", "党建思政", false));
        list.add(new Channel("student", "学生工作", false));
        list.add(new Channel("culture", "校园文化", false));
        list.add(new Channel("service", "公共服务", false));

        // 头条经典综合资讯频道 (供用户自由添加)
        list.add(new Channel("hot", "热点", false));
        list.add(new Channel("video_small", "小视频", false));
        list.add(new Channel("entertain", "娱乐", false));
        list.add(new Channel("beijing", "北京", false));
        list.add(new Channel("tech", "科技", false));
        list.add(new Channel("sports", "体育", false));
        return list;
    }

    /**
     * 获取当前保存在本地的【我的频道】
     */
    public static List<Channel> getMyChannels(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_MY_CHANNELS, null);
        if (json == null || json.trim().isEmpty()) {
            List<Channel> def = getDefaultMyChannels();
            saveMyChannels(context, def);
            return def;
        }
        return parseChannelsJson(json);
    }

    /**
     * 获取当前保存在本地的【更多频道】
     */
    public static List<Channel> getMoreChannels(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_MORE_CHANNELS, null);
        if (json == null || json.trim().isEmpty()) {
            List<Channel> def = getDefaultMoreChannels();
            saveMoreChannels(context, def);
            return def;
        }
        return parseChannelsJson(json);
    }

    /**
     * 保存所有频道变动 (我的频道 + 更多频道)
     */
    public static void saveAll(Context context, List<Channel> myChannels, List<Channel> moreChannels) {
        saveMyChannels(context, myChannels);
        saveMoreChannels(context, moreChannels);
    }

    public static void saveMyChannels(Context context, List<Channel> list) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_MY_CHANNELS, toJson(list)).apply();
    }

    public static void saveMoreChannels(Context context, List<Channel> list) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_MORE_CHANNELS, toJson(list)).apply();
    }

    private static String toJson(List<Channel> channels) {
        if (channels == null) return "[]";
        JSONArray array = new JSONArray();
        for (Channel c : channels) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", c.getId());
                obj.put("name", c.getName());
                obj.put("isFixed", c.isFixed());
                array.put(obj);
            } catch (Exception ignored) { }
        }
        return array.toString();
    }

    private static List<Channel> parseChannelsJson(String json) {
        List<Channel> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(new Channel(
                        obj.optString("id"),
                        obj.optString("name"),
                        obj.optBoolean("isFixed", false)
                ));
            }
        } catch (Exception e) {
            return getDefaultMyChannels();
        }
        return result;
    }
}
