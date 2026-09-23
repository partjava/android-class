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
 * 视频列表适配器。
 *
 * 单布局，写法和 ShopAdapter 完全一致：点击监听在 onCreateViewHolder 里
 * 只绑一次，回调出去的是**数据对象**而不是 position
 * （position 会随列表刷新失效，数据对象不会）。
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final List<VideoItem> videoList;

    public VideoAdapter(List<VideoItem> list) {
        this.videoList = list;
    }

    //点击回调，和 NewsMultiAdapter / ShopAdapter 同一套写法
    public interface OnItemClickListener {
        void onItemClick(VideoItem item);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvDuration, tvTitle, tvSource, tvPlay;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_video_cover);
            tvDuration = itemView.findViewById(R.id.tv_video_duration);
            tvTitle = itemView.findViewById(R.id.tv_video_title);
            tvSource = itemView.findViewById(R.id.tv_video_source);
            tvPlay = itemView.findViewById(R.id.tv_video_play);
        }
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        final VideoViewHolder holder = new VideoViewHolder(view);
        view.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && itemClickListener != null) {
                itemClickListener.onItemClick(videoList.get(position));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem item = videoList.get(position);
        holder.ivCover.setImageResource(item.getCoverRes());
        holder.tvDuration.setText(item.getDuration());
        holder.tvTitle.setText(item.getTitle());
        holder.tvSource.setText(item.getSource());
        holder.tvPlay.setText(item.getPlayCount());
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }
}
