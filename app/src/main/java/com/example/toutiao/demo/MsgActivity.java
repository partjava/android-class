package com.example.toutiao.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MsgActivity extends AppCompatActivity {
    private static final String[] PEERS = {"系统通知", "头条小助手", "创作小助手", "凝墨", "数码闲聊站", "客服中心", "老王", "头条官方"};
    private static final String[] SEEDS = {"[演示] 欢迎使用课程项目", "[演示] 你关注的内容有更新", "[演示] 欢迎交流创作心得", "[演示] 周末有空一起去看展吗？", "[演示] 欢迎交流数码话题", "[演示] 请描述你的问题", "[演示] 明天下午三点见", "[演示] 欢迎关注"};
    private final List<MsgItem> rows = new ArrayList<>();
    private ChatStore store;
    private MsgAdapter adapter;
    private String query = "";
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_msg); store = new ChatStore(this);
        for (int i = 0; i < PEERS.length; i++) store.seed(PEERS[i], SEEDS[i]);
        RecyclerView list = findViewById(R.id.rv_msg); list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MsgAdapter(rows); list.setAdapter(adapter);
        adapter.setOnItemClickListener(item -> open(item.getName()));
        findViewById(R.id.iv_msg_back).setOnClickListener(v -> finish());
        findViewById(R.id.iv_msg_search).setOnClickListener(v -> {
            EditText input = new EditText(this); input.setHint("会话名或消息内容"); input.setText(query);
            new AlertDialog.Builder(this).setTitle("搜索本地消息").setView(input)
                    .setPositiveButton("搜索", (d, w) -> { query = input.getText().toString().trim(); refresh(); })
                    .setNeutralButton("显示全部", (d, w) -> { query = ""; refresh(); }).setNegativeButton("取消", null).show();
        });
        findViewById(R.id.ll_entry_private).setOnClickListener(v -> { query = ""; refresh(); });
        findViewById(R.id.ll_entry_system).setOnClickListener(v -> open("系统通知"));
        for (int id : new int[]{R.id.ll_entry_like, R.id.ll_entry_fans}) findViewById(id).setOnClickListener(v ->
                new AlertDialog.Builder(this).setMessage("课程演示暂未接入赞、评论或粉丝通知。").setPositiveButton("知道了", null).show());
    }
    private void open(String name) { startActivity(new Intent(this, ChatActivity.class).putExtra(ChatActivity.EXTRA_PEER_NAME, name)); }
    @Override protected void onResume() { super.onResume(); refresh(); }
    private void refresh() {
        rows.clear(); int total = 0;
        for (String name : PEERS) {
            total += store.unread(name);
            if (name.contains(query) || store.preview(name).contains(query))
                rows.add(new MsgItem(name, store.preview(name), store.time(name), "系统通知".equals(name) ? R.drawable.avatar_system : R.drawable.avatar_blue, store.unread(name)));
        }
        adapter.notifyDataSetChanged();
        badge(R.id.badge_private, total - store.unread("系统通知")); badge(R.id.badge_system, store.unread("系统通知"));
    }
    private void badge(int id, int value) { TextView badge = findViewById(id); badge.setText(String.valueOf(value)); badge.setVisibility(value == 0 ? View.GONE : View.VISIBLE); }
}
