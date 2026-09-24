package com.example.toutiao.demo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class VideoFeedAdapter extends RecyclerView.Adapter<VideoFeedAdapter.FeedHolder> {

    private static final long TICK_MS = 200L;
    private static final int PROGRESS_MAX = 100;

    private static final long BURST_MS = 600L;

    private final List<VideoItem> data;

    private int padBottom;

    private RecyclerView attachedRv;

    private PagerSnapHelper snapHelper;
    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override public void onScrollStateChanged(@NonNull RecyclerView r,int state){if(state==RecyclerView.SCROLL_STATE_IDLE)syncToSnapView();}
    };

    private int currentPos;
    private int progress;
    private boolean playing;

    private boolean active;

    
    private VideoStore store;
    private boolean hostResumed = true;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!playing || !active || !hostResumed) return;
            FeedHolder h = holderForCurrent();
            if (h != null) { progress = h.player.progress(); renderProgress(); }
            handler.postDelayed(this, TICK_MS);
        }
    };

    public VideoFeedAdapter(List<VideoItem> data) {
        this.data = data;
    }

    
    public void setItemPaddingBottom(int px) {
        if (this.padBottom == px) return;
        this.padBottom = px;
        notifyDataSetChanged();

        if (attachedRv != null) {
            attachedRv.post(() -> {
                if(attachedRv==null)return;
                RecyclerView.LayoutManager lm = attachedRv.getLayoutManager();
                if (!(lm instanceof LinearLayoutManager)) return;
                int first = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                if (first != RecyclerView.NO_POSITION) attachedRv.scrollToPosition(first);
            });
        }
    }

    
    public void setActive(boolean wantActive) {
        if (active == wantActive) return;
        active = wantActive;
        if (active) {

            progress = 0;
            play();
        } else {
            pause();
            releasePlayers();
            progress = 0;
            renderProgress();
        }
    }

    
    public void onHostPause() {
        hostResumed = false;
        pause();
        releasePlayers();
    }

    public void onHostResume() {
        hostResumed = true;
        if (active && !playing) play();
    }

    
    public void release() {
        handler.removeCallbacksAndMessages(null);
        playing = false;
        releasePlayers();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        if (attachedRv == rv) return;   //同一个 RV，不重装（见类注释）
        attachedRv = rv;
        store = new VideoStore(rv.getContext());

        rv.setLayoutManager(new LinearLayoutManager(rv.getContext(),
                RecyclerView.VERTICAL, false));

        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rv);

        rv.addOnScrollListener(scrollListener);
        rv.post(() -> { if (attachedRv == rv && active && hostResumed) play(); });
    }

    
    private void syncToSnapView() {
        if (attachedRv == null || snapHelper == null) return;
        RecyclerView.LayoutManager lm = attachedRv.getLayoutManager();
        View snapped = snapHelper.findSnapView(lm);
        if (snapped == null) return;
        int pos = lm.getPosition(snapped);
        if (pos == RecyclerView.NO_POSITION || pos == currentPos) return;

        releasePlayers();
        int old = currentPos;
        currentPos = pos;

        progress = 0;

        notifyItemChanged(old);
        notifyItemChanged(currentPos);

        if (active) play();
    }

    private void play() {
        if (!active || !hostResumed) return;
        FeedHolder current = holderForCurrent();
        if (current != null) current.player.play();

        if (progress >= PROGRESS_MAX) {
            progress = 0;
            renderProgress();
        }
        playing = true;
        renderPlayIcon();
        handler.removeCallbacks(ticker);   //防叠：重复调 play() 不能排两次
        handler.postDelayed(ticker, TICK_MS);
    }

    private void pause() {
        FeedHolder current = holderForCurrent();
        if (current != null) current.player.pause();
        playing = false;
        renderPlayIcon();
        handler.removeCallbacks(ticker);
    }

    private void togglePlay() {
        if (playing) {
            pause();
        } else {
            play();
        }
    }

    private FeedHolder holderForCurrent() {
        if (attachedRv == null) return null;
        RecyclerView.ViewHolder vh =
                attachedRv.findViewHolderForAdapterPosition(currentPos);
        return (vh instanceof FeedHolder) ? (FeedHolder) vh : null;
    }

    
    private void renderProgress() {
        FeedHolder h = holderForCurrent();
        if (h != null) applyProgress(h, progress);
    }

    private void renderPlayIcon() {
        FeedHolder h = holderForCurrent();
        if (h == null) return;
        h.ivPlay.setImageResource(playing
                ? R.drawable.ic_pause_circle : R.drawable.ic_play_circle);
    }

    private void applyProgress(FeedHolder h, int value) {
        int trackWidth = h.flProgress.getWidth();

        if (trackWidth <= 0) return;
        ViewGroup.LayoutParams lp = h.vProgress.getLayoutParams();
        lp.width = trackWidth * value / PROGRESS_MAX;
        h.vProgress.setLayoutParams(lp);
    }

    
    private void like(FeedHolder holder, int pos, boolean wantLiked) {
        if (pos == RecyclerView.NO_POSITION) return;
        store.setLiked(data.get(pos).getId(), wantLiked);
        renderLike(holder, pos);
    }

    
    private void renderLike(FeedHolder holder, int position) {
        boolean isLiked = store.isLiked(data.get(position).getId());
        holder.ivLike.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                holder.itemView.getContext(), isLiked ? R.color.brand_red : R.color.on_brand)));
        holder.tvLike.setText(formatCount(
                data.get(position).getLikeCount() + (isLiked ? 1 : 0)));
    }

    
    private void playLikeBurst(FeedHolder holder) {
        ImageView burst = holder.ivLikeBurst;
        burst.animate().cancel();
        burst.setAlpha(1f);
        burst.setScaleX(0.4f);
        burst.setScaleY(0.4f);
        burst.animate()
                .alpha(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(BURST_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    
    private static String formatCount(int count) {
        if (count >= 10000) {
            return String.format(Locale.CHINA, "%.1f万", count / 10000.0);
        }
        return String.valueOf(count);
    }

    static class FeedHolder extends RecyclerView.ViewHolder {
        final OfflinePlayer player;
        final ImageView ivCover;
        final ImageView ivPlay;
        final TextView tvTitle;
        final TextView tvSource;
        final TextView tvDuration;
        final TextView tvPlay;
        final FrameLayout flProgress;
        final View vProgress;

        final TextView tvAvatar;
        final ImageView ivFollow;
        final ImageView ivLike;
        final TextView tvLike;
        final ImageView ivComment;
        final TextView tvComment;
        final ImageView ivCollect;
        final ImageView ivShare;
        final ImageView ivLikeBurst;

        FeedHolder(@NonNull View itemView) {
            super(itemView);
            player = itemView.findViewById(R.id.player_full);
            ivCover = itemView.findViewById(R.id.iv_full_cover);
            ivPlay = itemView.findViewById(R.id.iv_full_play);
            tvTitle = itemView.findViewById(R.id.tv_full_title);
            tvSource = itemView.findViewById(R.id.tv_full_source);
            tvDuration = itemView.findViewById(R.id.tv_full_duration);
            tvPlay = itemView.findViewById(R.id.tv_full_play);
            flProgress = itemView.findViewById(R.id.fl_full_progress);
            vProgress = itemView.findViewById(R.id.v_full_progress);

            tvAvatar = itemView.findViewById(R.id.tv_full_avatar);
            ivFollow = itemView.findViewById(R.id.iv_full_follow);
            ivLike = itemView.findViewById(R.id.iv_full_like);
            tvLike = itemView.findViewById(R.id.tv_full_like);
            ivComment = itemView.findViewById(R.id.iv_full_comment);
            tvComment = itemView.findViewById(R.id.tv_full_comment);
            ivCollect = itemView.findViewById(R.id.iv_full_collect);
            ivShare = itemView.findViewById(R.id.iv_full_share);
            ivLikeBurst = itemView.findViewById(R.id.iv_full_like_burst);
        }
    }

    @NonNull
    @Override
    public FeedHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_full, parent, false);
        FeedHolder holder = new FeedHolder(view);

        bindGesture(holder);

        bindRail(holder);
        return holder;
    }

    
    private void bindGesture(FeedHolder holder) {
        final GestureDetector detector = new GestureDetector(holder.itemView.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (isCurrent(holder)) togglePlay();
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (isCurrent(holder)) {

                    like(holder, holder.getAdapterPosition(), true);
                    playLikeBurst(holder);
                }
                return true;
            }
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return true;   //⚠️ 见上，不能改成 detector 的返回值
        });
    }

    
    private void bindRail(FeedHolder holder) {
        final Context context = holder.itemView.getContext();

        holder.ivLike.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            like(holder, pos, !store.isLiked(data.get(pos).getId()));
        });

        holder.ivComment.setOnClickListener(v -> {
            int pos=holder.getAdapterPosition(); if(pos==RecyclerView.NO_POSITION)return;
            VideoActions.comments(context, data.get(pos), store, () -> holder.tvComment.setText(String.valueOf(store.comments(data.get(pos).getId()).size())));
        });
        holder.ivCollect.setOnClickListener(v -> {
            int pos=holder.getAdapterPosition(); if(pos==RecyclerView.NO_POSITION)return;
            String id=data.get(pos).getId();store.setCollected(id,!store.isCollected(id));renderCollection(holder,pos);
        });
        holder.ivShare.setOnClickListener(v -> {int pos=holder.getAdapterPosition();if(pos!=RecyclerView.NO_POSITION)VideoActions.share(context,data.get(pos));});
        holder.ivFollow.setOnClickListener(v -> {
            store.setFollowing(!store.isFollowing());
            renderFollow(holder);
            toast(context,store.isFollowing()?"已关注离线影像课堂（本机）":"已取消关注");
        });
        holder.tvAvatar.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("离线影像课堂")
                .setMessage("本地课程创作账号\n\n提供3段原创生成的无声动画样片，用于演示视频播放与交互。样片不代表新闻事件的真实影像。关注、点赞与评论仅保存在本机。")
                .setPositiveButton("知道了",null).show());
    }

    private void toast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    
    private boolean isCurrent(FeedHolder holder) {
        int pos = holder.getAdapterPosition();
        return pos != RecyclerView.NO_POSITION && pos == currentPos;
    }

    @Override
    public void onBindViewHolder(@NonNull FeedHolder holder, int position) {
        VideoItem item = data.get(position);

        holder.itemView.setPadding(0, 0, 0, padBottom);

        holder.ivCover.setImageResource(item.getCoverRes());
        holder.tvTitle.setText(item.getTitle());
        holder.tvSource.setText(item.getSource());
        holder.tvDuration.setText(item.getDuration());
        holder.tvPlay.setText(item.getPlayCount());

        String source = item.getSource();
        holder.tvAvatar.setText(source.isEmpty() ? "视" : source.substring(0, 1));
        holder.tvComment.setText(String.valueOf(store.comments(item.getId()).size()));
        holder.tvLike.setTextColor(android.graphics.Color.WHITE);
        holder.tvComment.setTextColor(android.graphics.Color.WHITE);
        holder.tvLike.setShadowLayer(4,0,1,android.graphics.Color.BLACK);
        holder.tvComment.setShadowLayer(4,0,1,android.graphics.Color.BLACK);
        holder.itemView.findViewById(R.id.ll_full_rail).setBackgroundColor(0x99000000);
        renderCollection(holder,position);
        renderFollow(holder);
        holder.player.setResource(item.getVideoRes());
        if(position==currentPos && active && hostResumed && playing) holder.player.play(); else holder.player.release();

        renderLike(holder, position);

        holder.ivLikeBurst.animate().cancel();
        holder.ivLikeBurst.setAlpha(0f);
        holder.ivLikeBurst.setScaleX(1f);
        holder.ivLikeBurst.setScaleY(1f);

        boolean isCurrent = position == currentPos;
        holder.ivPlay.setImageResource(isCurrent && playing
                ? R.drawable.ic_pause_circle : R.drawable.ic_play_circle);

        final int pos = position;
        final int value = isCurrent ? progress : 0;
        holder.flProgress.post(() -> {

            if (holder.getAdapterPosition() == pos) applyProgress(holder, value);
        });
    }

    private void renderFollow(FeedHolder h) {
        h.ivFollow.setAlpha(store.isFollowing()?0.55f:1f);
        h.ivFollow.setContentDescription(store.isFollowing()?"取消关注":"关注离线影像课堂");
    }
    private void renderCollection(FeedHolder h,int pos) {
        boolean saved=store.isCollected(data.get(pos).getId());
        h.ivCollect.setImageTintList(ColorStateList.valueOf(saved?0xffffcc55:android.graphics.Color.WHITE));
        h.ivCollect.setContentDescription(saved?"取消收藏":"收藏");
    }
    private void releasePlayers() {
        if(attachedRv==null)return;
        for(int i=0;i<attachedRv.getChildCount();i++) {
            RecyclerView.ViewHolder vh=attachedRv.getChildViewHolder(attachedRv.getChildAt(i));
            if(vh instanceof FeedHolder)((FeedHolder)vh).player.release();
        }
    }
    @Override public void onViewRecycled(@NonNull FeedHolder h){h.player.release();super.onViewRecycled(h);}
    @Override public void onViewAttachedToWindow(@NonNull FeedHolder h){super.onViewAttachedToWindow(h);if(isCurrent(h)&&active&&hostResumed&&playing)h.player.play();}
    @Override public void onViewDetachedFromWindow(@NonNull FeedHolder h){h.player.release();super.onViewDetachedFromWindow(h);}
    @Override public void onDetachedFromRecyclerView(@NonNull RecyclerView rv){rv.removeOnScrollListener(scrollListener);releasePlayers();handler.removeCallbacks(ticker);if(snapHelper!=null)snapHelper.attachToRecyclerView(null);attachedRv=null;super.onDetachedFromRecyclerView(rv);}
    @Override
    public int getItemCount() {
        return data.size();
    }
}
