package com.example.toutiao.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天详情页：红顶栏 + 气泡列表 + 底部输入栏。
 *
 * 输入框是真的能发的：点「发送」把气泡追加到列表底部并清空输入框，
 * 一秒后再自动补一条对方回复。回复是本地定时器造的，不联网
 * —— 整个项目本来就没有任何网络请求。
 *
 * 全项目第一个有软键盘交互的页面，三个地方要留意：
 * · AndroidManifest 里必须有 windowSoftInputMode="adjustResize"，
 *   否则键盘弹出来会盖住输入栏
 * · 滚动到底要 post 一下再滚，原因见 scrollToBottom()
 * · 定时器要在 onDestroy 里清掉，原因见 onDestroy()
 */
public class ChatActivity extends AppCompatActivity {

    //会话名从消息页带过来
    public static final String EXTRA_PEER_NAME = "peer_name";

    //对方"回复"的延迟，单位毫秒
    private static final long REPLY_DELAY = 1000L;

    //轮流用的几句回复。只有一个会话，不接后台，随机反而显得乱，按下标轮着来
    private static final String[] AUTO_REPLIES = {
            "收到～", "好的，没问题", "哈哈哈，可以的", "我想想，晚点回你", "嗯嗯，那就这样定了"
    };

    private TextView tvTitle;
    private RecyclerView rvChat;
    private EditText etInput;
    private MaterialButton btnSend;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> msgList;

    //postDelayed 要用主线程 Looper；LocalBroadcast 之类的替代品在这里是杀鸡用牛刀
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int replyIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        bindView();
        initChatData();
        bindEvent();
    }

    private void bindView() {
        tvTitle = findViewById(R.id.tv_chat_title);
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_chat_send);
        findViewById(R.id.iv_chat_back).setOnClickListener(v -> finish());
    }

    private void initChatData() {
        String peer = getIntent().getStringExtra(EXTRA_PEER_NAME);
        //兜底：万一没带名字进来，标题也不至于是空的
        tvTitle.setText(peer == null ? "聊天" : peer);

        //先垫三句开场白，免得点进来是一片空白，左右两种气泡也能立刻看到
        msgList = new ArrayList<>();
        msgList.add(new ChatMessage("在吗？", false));
        msgList.add(new ChatMessage("在的，怎么了", true));
        msgList.add(new ChatMessage("周末有空一起去看展吗？", false));

        chatAdapter = new ChatAdapter(msgList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        //进来就停在最新一条
        scrollToBottom();
    }

    private void bindEvent() {
        btnSend.setOnClickListener(v -> send());

        //软键盘上那个回车键也当成发送（布局里 imeOptions="actionSend"）
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });
    }

    //把输入框里的内容发出去
    private void send() {
        String text = etInput.getText().toString().trim();
        //空消息直接丢掉，不然会往列表里塞一个看不见的空气泡
        if (text.isEmpty()) {
            return;
        }
        appendMessage(new ChatMessage(text, true));
        etInput.setText("");
        //一秒后补一条对方的回复
        handler.postDelayed(this::appendAutoReply, REPLY_DELAY);
    }

    private void appendAutoReply() {
        String reply = AUTO_REPLIES[replyIndex % AUTO_REPLIES.length];
        replyIndex++;
        appendMessage(new ChatMessage(reply, false));
    }

    private void appendMessage(ChatMessage message) {
        msgList.add(message);
        //新消息是从底部追加的，只刷新这一条，不整个 notifyDataSetChanged
        chatAdapter.notifyItemInserted(msgList.size() - 1);
        scrollToBottom();
    }

    /**
     * 滚到最新一条。
     *
     * 这里必须 post 一下再滚：notifyItemInserted 只是把新条目登记进去，
     * 此刻它还**没有被测量**，RecyclerView 并不知道它有多高，
     * 直接 scrollToPosition 会停在倒数第二条上（看着就是"没滚到底"）。
     * post 的回调排在布局之后执行，那时高度才是准的。
     */
    private void scrollToBottom() {
        rvChat.post(() -> rvChat.scrollToPosition(msgList.size() - 1));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //把还没触发的模拟回复清掉。不清的话，用户发完消息立刻退出页面，
        //一秒后那个 Runnable 仍会执行，而它持有这个 Activity 的引用
        //（this::appendAutoReply），Activity 就没法被回收了。
        handler.removeCallbacksAndMessages(null);
    }
}
