package com.example.toutiao.demo;

/**
 * 聊天页里的一条消息。
 *
 * 只有两个字段，因为气泡布局的差异全都由 mine 这一个布尔值决定：
 * true 走右边的红色气泡，false 走左边的白色气泡。
 */
public class ChatMessage {

    private final String text;
    private final boolean mine; // true = 我方（右侧红气泡）

    public ChatMessage(String text, boolean mine) {
        this.text = text;
        this.mine = mine;
    }

    public String getText() {
        return text;
    }

    public boolean isMine() {
        return mine;
    }
}
