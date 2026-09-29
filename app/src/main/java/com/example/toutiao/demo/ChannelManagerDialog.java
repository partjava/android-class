package com.example.toutiao.demo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 频道管理与编辑弹窗 (ChannelManagerDialog)。
 * 完美还原今日头条风格：
 * 1. 顶部关闭与标题
 * 2. 【我的频道】：显示已选频道，高亮当前频道，支持点击切换频道或进入编辑模式一键移除
 * 3. 【更多频道】：显示晴川官网其他版块及更多精彩频道，带“+”号角标，点击一键添加进我的频道
 */
public class ChannelManagerDialog {

    public interface OnChannelChangeListener {
        void onChannelSelected(Channel channel);
        void onChannelsUpdated(List<Channel> myChannels);
    }

    public static void show(Context context, String currentChannelId, OnChannelChangeListener listener) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_channel_manager, null);
        dialog.setContentView(root);

        List<Channel> myChannels = ChannelStore.getMyChannels(context);
        List<Channel> moreChannels = ChannelStore.getMoreChannels(context);

        RecyclerView rvMy = root.findViewById(R.id.rv_my_channels);
        RecyclerView rvMore = root.findViewById(R.id.rv_more_channels);
        TextView btnEdit = root.findViewById(R.id.btn_edit_channels);
        TextView tvMyTip = root.findViewById(R.id.tv_my_channel_tip);

        rvMy.setLayoutManager(new GridLayoutManager(context, 4));
        rvMore.setLayoutManager(new GridLayoutManager(context, 4));

        final boolean[] isEditMode = {false};

        final MyChannelsAdapter[] myAdapterRef = new MyChannelsAdapter[1];
        final MoreChannelsAdapter[] moreAdapterRef = new MoreChannelsAdapter[1];

        // 我的频道适配器
        myAdapterRef[0] = new MyChannelsAdapter(context, myChannels, currentChannelId, isEditMode, new MyChannelsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Channel channel, int position) {
                if (isEditMode[0]) {
                    // 编辑模式：点击非固定频道直接移除
                    if (!channel.isFixed()) {
                        myChannels.remove(position);
                        moreChannels.add(0, channel);
                        ChannelStore.saveAll(context, myChannels, moreChannels);
                        if (listener != null) listener.onChannelsUpdated(myChannels);
                        if (myAdapterRef[0] != null) myAdapterRef[0].notifyDataSetChanged();
                        if (moreAdapterRef[0] != null) moreAdapterRef[0].notifyDataSetChanged();
                    }
                } else {
                    // 浏览模式：点击直接选中进入该频道
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onChannelSelected(channel);
                    }
                }
            }

            @Override
            public void onDeleteClick(Channel channel, int position) {
                if (!channel.isFixed()) {
                    myChannels.remove(position);
                    moreChannels.add(0, channel);
                    ChannelStore.saveAll(context, myChannels, moreChannels);
                    if (listener != null) listener.onChannelsUpdated(myChannels);
                    if (myAdapterRef[0] != null) myAdapterRef[0].notifyDataSetChanged();
                    if (moreAdapterRef[0] != null) moreAdapterRef[0].notifyDataSetChanged();
                }
            }
        });
        rvMy.setAdapter(myAdapterRef[0]);

        // 更多频道适配器
        moreAdapterRef[0] = new MoreChannelsAdapter(context, moreChannels, channel -> {
            android.util.Log.d("ChannelDialog", "onAddClick adding: " + channel.getName());
            moreChannels.remove(channel);
            myChannels.add(channel);
            ChannelStore.saveAll(context, myChannels, moreChannels);
            if (listener != null) listener.onChannelsUpdated(myChannels);
            if (myAdapterRef[0] != null) myAdapterRef[0].notifyDataSetChanged();
            if (moreAdapterRef[0] != null) moreAdapterRef[0].notifyDataSetChanged();
        });
        rvMore.setAdapter(moreAdapterRef[0]);

        // 编辑/完成模式切换
        btnEdit.setOnClickListener(v -> {
            isEditMode[0] = !isEditMode[0];
            if (isEditMode[0]) {
                btnEdit.setText("完成");
                tvMyTip.setText("点击右上角删除或调整频道");
            } else {
                btnEdit.setText("编辑");
                tvMyTip.setText("点击进入频道");
            }
            if (myAdapterRef[0] != null) {
                myAdapterRef[0].notifyDataSetChanged();
            }
        });

        // 关闭弹窗
        root.findViewById(R.id.iv_close_channel_dialog).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.white);
        }
    }

    // =========================================================================
    // 我的频道适配器
    // =========================================================================
    private static class MyChannelsAdapter extends RecyclerView.Adapter<MyChannelsAdapter.ViewHolder> {
        interface OnItemClickListener {
            void onItemClick(Channel channel, int position);
            void onDeleteClick(Channel channel, int position);
        }

        private final Context context;
        private final List<Channel> list;
        private final String activeChannelId;
        private final boolean[] isEditMode;
        private final OnItemClickListener listener;

        MyChannelsAdapter(Context context, List<Channel> list, String activeChannelId, boolean[] isEditMode, OnItemClickListener listener) {
            this.context = context;
            this.list = list;
            this.activeChannelId = activeChannelId;
            this.isEditMode = isEditMode;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_channel_chip, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Channel channel = list.get(position);
            holder.tvName.setText(channel.getName());

            boolean isActive = channel.getId().equals(activeChannelId);
            if (isActive && !isEditMode[0]) {
                holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.brand_red));
                holder.tvName.getPaint().setFakeBoldText(true);
            } else {
                holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                holder.tvName.getPaint().setFakeBoldText(false);
            }

            // 编辑模式下非固定频道展示删除叉号
            if (isEditMode[0] && !channel.isFixed()) {
                holder.tvDelBadge.setVisibility(View.VISIBLE);
                holder.tvDelBadge.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && listener != null) {
                        listener.onDeleteClick(list.get(pos), pos);
                    }
                });
            } else {
                holder.tvDelBadge.setVisibility(View.GONE);
            }

            View.OnClickListener clickListener = v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(list.get(pos), pos);
                }
            };
            holder.itemView.setOnClickListener(clickListener);
            holder.tvName.setOnClickListener(clickListener);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvDelBadge;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_channel_name);
                tvDelBadge = itemView.findViewById(R.id.tv_channel_del_badge);
            }
        }
    }

    // =========================================================================
    // 更多频道适配器
    // =========================================================================
    private static class MoreChannelsAdapter extends RecyclerView.Adapter<MoreChannelsAdapter.ViewHolder> {
        interface OnItemClickListener {
            void onAddClick(Channel channel);
        }

        private final Context context;
        private final List<Channel> list;
        private final OnItemClickListener listener;

        MoreChannelsAdapter(Context context, List<Channel> list, OnItemClickListener listener) {
            this.context = context;
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_channel_chip, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Channel channel = list.get(position);
            holder.tvName.setText(channel.getName() + " +");
            holder.tvName.setTextColor(Color.parseColor("#555555"));
            holder.tvDelBadge.setVisibility(View.GONE);

            View.OnClickListener clickListener = v -> {
                int pos = holder.getAdapterPosition();
                android.util.Log.d("ChannelDialog", "MoreChannels clicked pos=" + pos);
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAddClick(list.get(pos));
                }
            };
            holder.itemView.setOnClickListener(clickListener);
            holder.tvName.setOnClickListener(clickListener);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvDelBadge;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_channel_name);
                tvDelBadge = itemView.findViewById(R.id.tv_channel_del_badge);
            }
        }
    }
}
