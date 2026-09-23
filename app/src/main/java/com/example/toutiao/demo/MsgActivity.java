package com.example.toutiao.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息页，从「我的」页的两个入口进来。
 *
 * 三段结构：
 * · 顶栏：返回 + 「消息」+ 搜索图标（白底，二级页用白顶栏）
 * · 四宫格功能入口：私信 / 赞和评论 / 粉丝 / 系统通知，前三个带未读角标
 * · 会话列表：头像 + 名字 + 消息预览 + 时间 + 未读角标
 *
 * 这是二级页，所以没有底部导航 —— 顶栏有返回键就够了。
 * 结构照 ShopActivity：bindView() → initXxxData() → bindEvent()。
 */
public class MsgActivity extends AppCompatActivity {

    private ImageView ivBack, ivSearch;
    private View llEntryPrivate, llEntryLike, llEntryFans, llEntrySystem;
    private RecyclerView rvMsg;

    private MsgAdapter msgAdapter;
    private List<MsgItem> msgList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg);
        bindView();
        initMsgData();
        bindEvent();
    }

    private void bindView() {
        ivBack = findViewById(R.id.iv_msg_back);
        ivSearch = findViewById(R.id.iv_msg_search);
        llEntryPrivate = findViewById(R.id.ll_entry_private);
        llEntryLike = findViewById(R.id.ll_entry_like);
        llEntryFans = findViewById(R.id.ll_entry_fans);
        llEntrySystem = findViewById(R.id.ll_entry_system);
        rvMsg = findViewById(R.id.rv_msg);
    }

    /**
     * 会话数据，全部写死在代码里。
     *
     * 头像是 drawable 里画好的一组矢量图（avatar_blue/green/orange/purple
     * 是彩色圆底 + 白色人形，avatar_system 是红底 + 铃铛）。
     * 刻意不用 image1~8 那批照片：它们是风景照，裁成圆形既没有脸、
     * 构图也怪，那些图本来就是给首页大国工匠列表当 120x90 缩略图用的。
     */
    private void initMsgData() {
        msgList = new ArrayList<>();
        msgList.add(new MsgItem("系统通知", "你的账号已登录，欢迎回来",
                "10:24", R.drawable.avatar_system, 2));
        msgList.add(new MsgItem("头条小助手", "你关注的内容有更新，快来看看",
                "09:15", R.drawable.avatar_blue, 0));
        msgList.add(new MsgItem("创作小助手", "你的作品《秋日随笔》已通过审核",
                "昨天", R.drawable.avatar_orange, 1));
        msgList.add(new MsgItem("凝墨", "好的，那就这么定了",
                "昨天", R.drawable.avatar_green, 3));
        msgList.add(new MsgItem("数码闲聊站", "群里的新消息：这代续航确实顶",
                "昨天", R.drawable.avatar_purple, 0));
        msgList.add(new MsgItem("客服中心", "您反馈的问题已受理，请耐心等待",
                "周一", R.drawable.avatar_blue, 0));
        msgList.add(new MsgItem("老王", "明天下午三点，老地方见",
                "周一", R.drawable.avatar_green, 0));
        msgList.add(new MsgItem("头条官方", "恭喜你获得「优质创作者」认证",
                "周日", R.drawable.avatar_system, 0));

        msgAdapter = new MsgAdapter(msgList);
        rvMsg.setLayoutManager(new LinearLayoutManager(this));
        rvMsg.setAdapter(msgAdapter);

        //点会话进聊天页，把对方名字带过去当标题
        msgAdapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(MsgActivity.this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_PEER_NAME, item.getName());
            startActivity(intent);
        });
    }

    private void bindEvent() {
        //二级页的返回键，直接结束自己
        ivBack.setOnClickListener(v -> finish());

        //剩下的都是演示用的占位入口，和我的页那几个九宫格入口写法一致
        ivSearch.setOnClickListener(v ->
                Toast.makeText(this, "搜索消息", Toast.LENGTH_SHORT).show());
        llEntryPrivate.setOnClickListener(v ->
                Toast.makeText(this, "私信", Toast.LENGTH_SHORT).show());
        llEntryLike.setOnClickListener(v ->
                Toast.makeText(this, "赞和评论", Toast.LENGTH_SHORT).show());
        llEntryFans.setOnClickListener(v ->
                Toast.makeText(this, "粉丝", Toast.LENGTH_SHORT).show());
        llEntrySystem.setOnClickListener(v ->
                Toast.makeText(this, "系统通知", Toast.LENGTH_SHORT).show());
    }
}
