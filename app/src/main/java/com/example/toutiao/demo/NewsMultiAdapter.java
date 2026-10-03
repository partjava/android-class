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
    private String highlightQuery = "";

    public NewsMultiAdapter(List<News> list) {
        this.newsList = list;
    }

    public void setHighlightQuery(String query) {
        this.highlightQuery = (query == null ? "" : query.trim());
        notifyDataSetChanged();
    }

    private CharSequence formatHighlight(String text, android.content.Context context) {
        if (text == null || highlightQuery.isEmpty()) return text == null ? "" : text;
        int idx = text.toLowerCase(java.util.Locale.ROOT).indexOf(highlightQuery.toLowerCase(java.util.Locale.ROOT));
        if (idx == -1) return text;
        android.text.SpannableString ss = new android.text.SpannableString(text);
        int color = androidx.core.content.ContextCompat.getColor(context, R.color.brand_red);
        while (idx >= 0) {
            ss.setSpan(new android.text.style.ForegroundColorSpan(color), idx, idx + highlightQuery.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), idx, idx + highlightQuery.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            idx = text.toLowerCase(java.util.Locale.ROOT).indexOf(highlightQuery.toLowerCase(java.util.Locale.ROOT), idx + highlightQuery.length());
        }
        return ss;
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

    static class BigImgViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle,tvSource,tvTime;
        ImageView ivBig;
        public BigImgViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title_big);
            tvSource = itemView.findViewById(R.id.tv_source_big);
            tvTime = itemView.findViewById(R.id.tv_time_big);
            ivBig = itemView.findViewById(R.id.iv_big);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder holder;
        switch (viewType){
            case News.TYPE_BIG_IMG:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_big,parent,false);
                holder = new BigImgViewHolder(view);
                break;
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
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        News news = newsList.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(news);
            }
        });
        int type = news.getType();
        android.content.Context ctx = holder.itemView.getContext();
        CharSequence highlightedTitle = formatHighlight(news.getTitle(), ctx);

        switch (type){
            case News.TYPE_TEXT:
                TextViewHolder textHolder = (TextViewHolder) holder;
                textHolder.tvTitle.setText(highlightedTitle);
                textHolder.tvSource.setText(news.getSource());
                textHolder.tvTime.setText(news.getTime());
                break;
            case News.TYPE_SINGLE_IMG:
                SingleImgViewHolder singleHolder = (SingleImgViewHolder) holder;
                singleHolder.tvTitle.setText(highlightedTitle);
                singleHolder.tvSource.setText(news.getSource());
                singleHolder.tvTime.setText(news.getTime());
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    RemoteImage.load(singleHolder.ivSingle, news.getImageUrl());
                } else if (news.getImg1() != 0) {
                    singleHolder.ivSingle.setImageResource(news.getImg1());
                }
                break;
            case News.TYPE_BIG_IMG:
                BigImgViewHolder bigHolder = (BigImgViewHolder) holder;
                bigHolder.tvTitle.setText(highlightedTitle);
                bigHolder.tvSource.setText(news.getSource());
                bigHolder.tvTime.setText(news.getTime());
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    RemoteImage.load(bigHolder.ivBig, news.getImageUrl());
                } else if (news.getImg1() != 0) {
                    bigHolder.ivBig.setImageResource(news.getImg1());
                }
                break;
            case News.TYPE_THREE_IMG:
                ThreeImgViewHolder threeHolder = (ThreeImgViewHolder) holder;
                threeHolder.tvTitle.setText(highlightedTitle);
                threeHolder.tvSource.setText(news.getSource());
                threeHolder.tvTime.setText(news.getTime());
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    RemoteImage.load(threeHolder.iv1, news.getImageUrl());
                } else if (news.getImg1() != 0) {
                    threeHolder.iv1.setImageResource(news.getImg1());
                }
                if (news.getImageUrl2() != null && !news.getImageUrl2().isEmpty()) {
                    RemoteImage.load(threeHolder.iv2, news.getImageUrl2());
                } else if (news.getImg2() != 0) {
                    threeHolder.iv2.setImageResource(news.getImg2());
                }
                if (news.getImageUrl3() != null && !news.getImageUrl3().isEmpty()) {
                    RemoteImage.load(threeHolder.iv3, news.getImageUrl3());
                } else if (news.getImg3() != 0) {
                    threeHolder.iv3.setImageResource(news.getImg3());
                }
                break;
            case News.TYPE_VIDEO:
                VideoViewHolder videoHolder = (VideoViewHolder) holder;
                videoHolder.tvTitle.setText(highlightedTitle);
                videoHolder.tvSource.setText(news.getSource());
                videoHolder.tvTime.setText(news.getTime());
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    RemoteImage.load(videoHolder.ivCover, news.getImageUrl());
                } else if (news.getImg1() != 0) {
                    videoHolder.ivCover.setImageResource(news.getImg1());
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }
}
