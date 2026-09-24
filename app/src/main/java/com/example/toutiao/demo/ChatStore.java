package com.example.toutiao.demo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Each peer has its own persisted history, preview and unread count. */
public final class ChatStore {
    private final SharedPreferences prefs;
    public ChatStore(Context context) { this(context, "chat_history"); }
    ChatStore(Context context, String name) { prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE); }
    public List<ChatMessage> messages(String peer) {
        List<ChatMessage> result = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(prefs.getString("messages:" + peer, "[]"));
            for (int i = 0; i < values.length(); i++) {
                JSONObject value = values.getJSONObject(i);
                result.add(new ChatMessage(value.getString("text"), value.getBoolean("mine")));
            }
        } catch (JSONException ignored) { /* Recover malformed local history as an empty conversation. */ }
        return result;
    }
    public void append(String peer, ChatMessage message) {
        if (message.getText().trim().isEmpty()) return;
        List<ChatMessage> values = messages(peer);
        values.add(message);
        JSONArray json = new JSONArray();
        try {
            for (ChatMessage value : values) {
                JSONObject row = new JSONObject();
                row.put("text", value.getText()); row.put("mine", value.isMine()); json.put(row);
            }
        } catch (JSONException e) { throw new IllegalStateException(e); }
        prefs.edit().putString("messages:" + peer, json.toString())
                .putLong("time:" + peer, System.currentTimeMillis())
                .putInt("unread:" + peer, unread(peer) + (message.isMine() ? 0 : 1)).apply();
    }
    public void seed(String peer, String text) {
        if (!prefs.contains("messages:" + peer)) append(peer, new ChatMessage(text, false));
    }
    public String preview(String peer) {
        List<ChatMessage> values = messages(peer);
        return values.isEmpty() ? "暂无消息" : values.get(values.size() - 1).getText();
    }
    public String time(String peer) {
        long value = prefs.getLong("time:" + peer, 0);
        return value == 0 ? "" : new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(value));
    }
    public int unread(String peer) { return prefs.getInt("unread:" + peer, 0); }
    public void markRead(String peer) { prefs.edit().putInt("unread:" + peer, 0).apply(); }
}
