package com.example.toutiao.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NewsMultiAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<News> newsList;

    public NewsMultiAdapter(List<News> list) {
        this.newsList = list;
    }

    //点击回调。
    //RecyclerView 不像 ListView 那样自带 setOnItemClickListener，
    //需要自己开一个接口，由使用方实现。
    public interface OnItemClickListener {
        void onItemClick(News news);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return newsList.get(position).getType();
    }

    //4种ViewHolder
    static class TextViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle,tvSource,tvTime;
        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title_text);
            tvSource = itemView.findViewById(R.id.tv_source_text);
            tvTime = itemView.findViewById(R.id.tv_time_text);
        }
    }
    static class SingleImgViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle,tvSource,tvTime;
        ImageView ivSingle;
        public SingleImgViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title_single);
            tvSource = itemView.findViewById(R.id.tv_source_single);
            tvTime = itemView.findViewById(R.id.tv_time_single);
            ivSingle = itemView.findViewById(R.id.iv_single);
        }
    }
    static class ThreeImgViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle,tvSource,tvTime;
        ImageView iv1,iv2,iv3;
        public ThreeImgViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title_three);
            tvSource = itemView.findViewById(R.id.tv_source_three);
            tvTime = itemView.findViewById(R.id.tv_time_three);
            iv1 = itemView.findViewById(R.id.iv_three1);
            iv2 = itemView.findViewById(R.id.iv_three2);
            iv3 = itemView.findViewById(R.id.iv_three3);
        }
    }
    static class VideoViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle,tvSource,tvTime;
        ImageView ivCover;
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title_video);
            tvSource = itemView.findViewById(R.id.tv_source_video);
            tvTime = itemView.findViewById(R.id.tv_time_video);
            ivCover = itemView.findViewById(R.id.iv_video_cover);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder holder;
        switch (viewType){
            case News.TYPE_SINGLE_IMG:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_single,parent,false);
                holder = new SingleImgViewHolder(view);
                break;
            case News.TYPE_THREE_IMG:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_three,parent,false);
                holder = new ThreeImgViewHolder(view);
                break;
            case News.TYPE_VIDEO:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_video,parent,false);
                holder = new VideoViewHolder(view);
                break;
            case News.TYPE_TEXT:
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_text,parent,false);
                holder = new TextViewHolder(view);
                break;
        }
        //4 种布局共用同一套点击处理
        view.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && itemClickListener != null) {
                itemClickListener.onItemClick(newsList.get(position));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        News news = newsList.get(position);
        int type = news.getType();
        switch (type){
            case News.TYPE_TEXT:
                TextViewHolder textHolder = (TextViewHolder) holder;
                textHolder.tvTitle.setText(news.getTitle());
                textHolder.tvSource.setText(news.getSource());
                textHolder.tvTime.setText(news.getTime());
                break;
            case News.TYPE_SINGLE_IMG:
                SingleImgViewHolder singleHolder = (SingleImgViewHolder) holder;
                singleHolder.tvTitle.setText(news.getTitle());
                singleHolder.tvSource.setText(news.getSource());
                singleHolder.tvTime.setText(news.getTime());
                singleHolder.ivSingle.setImageResource(news.getImg1());
                break;
            case News.TYPE_THREE_IMG:
                ThreeImgViewHolder threeHolder = (ThreeImgViewHolder) holder;
                threeHolder.tvTitle.setText(news.getTitle());
                threeHolder.tvSource.setText(news.getSource());
                threeHolder.tvTime.setText(news.getTime());
                threeHolder.iv1.setImageResource(news.getImg1());
                threeHolder.iv2.setImageResource(news.getImg2());
                threeHolder.iv3.setImageResource(news.getImg3());
                break;
            case News.TYPE_VIDEO:
                VideoViewHolder videoHolder = (VideoViewHolder) holder;
                videoHolder.tvTitle.setText(news.getTitle());
                videoHolder.tvSource.setText(news.getSource());
                videoHolder.tvTime.setText(news.getTime());
                videoHolder.ivCover.setImageResource(news.getImg1());
                break;
        }
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }
}
