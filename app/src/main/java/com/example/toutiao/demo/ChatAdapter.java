package com.example.toutiao.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 聊天页的适配器，一个列表两种气泡布局。
 *
 * 和 NewsMultiAdapter 那种"按业务类型分 4 种布局"不同，
 * 这里只按消息是不是自己发的分左右两种 —— 右边红气泡、左边白气泡。
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final int TYPE_LEFT = 0;   // 对方，白气泡
    private static final int TYPE_RIGHT = 1;  // 自己，红气泡

    private final List<ChatMessage> messageList;

    public ChatAdapter(List<ChatMessage> list) {
        this.messageList = list;
    }

    //左右两种气泡的布局不一样，但里面都是同一个 TextView，共用一个 ViewHolder
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_bubble_text);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isMine() ? TYPE_RIGHT : TYPE_LEFT;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_RIGHT
                ? R.layout.item_chat_right
                : R.layout.item_chat_left;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.tvText.setText(messageList.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
}
