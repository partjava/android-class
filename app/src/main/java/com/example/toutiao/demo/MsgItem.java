package com.example.toutiao.demo;

/**
 * 消息页会话列表的一条数据。
 *
 * 写法和 ShopItem 一致：final 字段 + 全参构造 + 每个字段一个 getter。
 * 数据是代码里写死的演示数据，不需要 setter。
 */
public class MsgItem {

    private final String name;      // 会话名，也是点进去之后聊天页的标题
    private final String lastMsg;   // 最后一条消息的预览文字
    private final String time;      // 最后一条消息的时间："10:24" / "昨天" / "周一"
    private final int avatarRes;    // 头像，指向 avatar*.xml 里画好的那一组
    private final int unread;       // 未读条数，0 表示这条不显示角标

    public MsgItem(String name, String lastMsg, String time, int avatarRes, int unread) {
        this.name = name;
        this.lastMsg = lastMsg;
        this.time = time;
        this.avatarRes = avatarRes;
        this.unread = unread;
    }

    public String getName() {
        return name;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public String getTime() {
        return time;
    }

    public int getAvatarRes() {
        return avatarRes;
    }

    public int getUnread() {
        return unread;
    }
}
