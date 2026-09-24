package com.example.toutiao.demo;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** Local demo conversation: sending stores both bubbles during the UI event. */
public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_PEER_NAME = "peer_name";
    private ChatStore store;
    private String peer;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;
    private RecyclerView list;
    private EditText input;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_chat);
        store = new ChatStore(this);
        peer = getIntent().getStringExtra(EXTRA_PEER_NAME);
        if (peer == null || peer.trim().isEmpty()) peer = "头条小助手";
        ((TextView)findViewById(R.id.tv_chat_title)).setText(peer + "（本地演示）");
        findViewById(R.id.iv_chat_back).setOnClickListener(v -> finish());
        list = findViewById(R.id.rv_chat); input = findViewById(R.id.et_chat_input);
        messages = store.messages(peer); store.markRead(peer);
        adapter = new ChatAdapter(messages); list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        findViewById(R.id.btn_chat_send).setOnClickListener(v -> send());
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND) { send(); return true; } return false;
        });
        scroll();
    }
    private void send() {
        String text = input.getText().toString().trim(); if (text.isEmpty()) return;
        append(new ChatMessage(text, true));
        append(new ChatMessage("[模拟回复] 收到：" + text, false));
        store.markRead(peer); input.setText(""); scroll();
    }
    private void append(ChatMessage message) {
        store.append(peer, message); messages.add(message); adapter.notifyItemInserted(messages.size() - 1);
    }
    private void scroll() { if (!messages.isEmpty()) list.post(() -> list.scrollToPosition(messages.size() - 1)); }
}
