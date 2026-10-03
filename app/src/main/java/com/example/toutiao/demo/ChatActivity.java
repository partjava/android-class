package com.example.toutiao.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 聊天与智能校园助手详情页。
 * 内置晴川校园问答知识库，支持高频校园生活与教学问题智能应答，支持快捷标签提问与仿真思考延迟。
 */
public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_PEER_NAME = "peer_name";
    private ChatStore store;
    private String peer;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;
    private RecyclerView list;
    private EditText input;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_chat);
        store = new ChatStore(this);
        peer = getIntent().getStringExtra(EXTRA_PEER_NAME);
        if (peer == null || peer.trim().isEmpty()) peer = "晴川小助手";

        TextView tvTitle = findViewById(R.id.tv_chat_title);
        if (peer.contains("晴川") || peer.contains("助手") || peer.contains("客服")) {
            tvTitle.setText(peer + " · 智能问答");
        } else {
            tvTitle.setText(peer);
        }

        findViewById(R.id.iv_chat_back).setOnClickListener(v -> finish());
        list = findViewById(R.id.rv_chat);
        input = findViewById(R.id.et_chat_input);
        messages = store.messages(peer);
        store.markRead(peer);

        adapter = new ChatAdapter(messages);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.btn_chat_send).setOnClickListener(v -> send());
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });

        initQuickChips();
        scroll();
    }

    private void initQuickChips() {
        LinearLayout llChips = findViewById(R.id.ll_quick_chips);
        if (llChips == null) return;
        llChips.removeAllViews();

        for (String q : QingchuanQaEngine.QUICK_QUESTIONS) {
            TextView chip = new TextView(this);
            chip.setText(q);
            chip.setTextSize(12.5f);
            chip.setTextColor(getResources().getColor(R.color.brand_red));
            chip.setBackgroundResource(R.drawable.bg_shop_card);
            chip.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dpToPx(8);
            chip.setLayoutParams(lp);

            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> sendText(q));

            llChips.addView(chip);
        }
    }

    private void send() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        sendText(text);
    }

    private void sendText(String text) {
        append(new ChatMessage(text, true));
        scroll();

        // 模拟智能小助手打字思考动效（延迟 500ms 回复）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String reply;
            if (peer != null && (peer.contains("晴川") || peer.contains("助手") || peer.contains("客服") || peer.contains("官方"))) {
                reply = QingchuanQaEngine.answer(text);
            } else {
                if (text.contains("图") || text.contains("课") || text.contains("作息") || text.contains("网") || text.contains("晴川") || text.contains("电话")) {
                    reply = QingchuanQaEngine.answer(text);
                } else {
                    reply = "收到您的消息：“" + text + "”。如果有关于武汉晴川学院图书馆、教务教学或宿舍作息等问题，随时问我哦！";
                }
            }
            append(new ChatMessage(reply, false));
            store.markRead(peer);
            scroll();
        }, 500);
    }

    private void append(ChatMessage message) {
        store.append(peer, message);
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
    }

    private void scroll() {
        if (!messages.isEmpty()) {
            list.post(() -> list.scrollToPosition(messages.size() - 1));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
