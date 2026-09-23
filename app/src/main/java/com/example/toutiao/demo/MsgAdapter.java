package com.example.toutiao.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 消息页会话列表的适配器。
 *
 * 结构和 ShopAdapter 完全一致：ViewHolder 是 static 嵌套类，
 * 点击事件在 onCreateViewHolder 里绑一次（而不是在 onBindViewHolder 里
 * 反复 setOnClickListener），回调直接把整个 MsgItem 交出去。
 */
public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.MsgViewHolder> {

    private final List<MsgItem> itemList;

    public MsgAdapter(List<MsgItem> list) {
        this.itemList = list;
    }

    //点击回调，和 NewsMultiAdapter / ShopAdapter 同一套写法
    public interface OnItemClickListener {
        void onItemClick(MsgItem item);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    static class MsgViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvLastMsg, tvTime, tvUnread;

        MsgViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_msg_avatar);
            tvName = itemView.findViewById(R.id.tv_msg_name);
            tvLastMsg = itemView.findViewById(R.id.tv_msg_last);
            tvTime = itemView.findViewById(R.id.tv_msg_time);
            tvUnread = itemView.findViewById(R.id.tv_msg_unread);
        }
    }

    @NonNull
    @Override
    public MsgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_msg, parent, false);
        final MsgViewHolder holder = new MsgViewHolder(view);
        view.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && itemClickListener != null) {
                itemClickListener.onItemClick(itemList.get(position));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MsgViewHolder holder, int position) {
        MsgItem item = itemList.get(position);
        holder.ivAvatar.setImageResource(item.getAvatarRes());
        holder.tvName.setText(item.getName());
        holder.tvLastMsg.setText(item.getLastMsg());
        holder.tvTime.setText(item.getTime());

        //没未读的会话不显示角标。用 GONE 而不是 INVISIBLE ——
        //INVISIBLE 还占着位置，右边会多出一块空白，时间就不贴边了。
        if (item.getUnread() > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(String.valueOf(item.getUnread()));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
