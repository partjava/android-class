package com.example.toutiao.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * 频道分页适配器：一个频道 = ViewPager2 里的一页。
 *
 * 这一层只干一件事——把「第几页」翻译成「哪一份数据」，页里的列表仍然
 * 交给 VideoAdapter 去画（那一份这次一行都没改）。
 *
 * ⚠️ 每一页各自持有一个独立的 VideoAdapter，吃自己那一份 channelData。
 *    这件事**不是风格选择，是必须这么写**：
 *
 *    改造前用的是「一个共享的 videoList + 切频道时 clear/addAll/
 *    notifyDataSetChanged，adapter 持有同一个引用」那套（首页切换新闻标签、
 *    商城切换品类也都是那套）。那套在单页列表里没问题，搬进分页就会坏——
 *    ViewPager2 横滑时相邻两页**同时存活**，它们会拿到同一个 videoList
 *    对象，于是两页内容一模一样；而且滑到一半被 notifyDataSetChanged
 *    打断，正在跑的滑动动画会被顶掉。
 *
 *    所以这里把「换数据」整个删掉了：数据在构造时就分好，切页只是换页。
 *
 * ⚠️ 这也是 VideoActivity 里那套 clear/addAll/notifyDataSetChanged/
 *    scrollToPosition(0) 整段消失的原因——不是搬走了，是不需要了。
 *    （副作用：切频道不再强制回到列表顶部。分页的固有行为，README 已记。）
 */
public class VideoChannelAdapter extends RecyclerView.Adapter<VideoChannelAdapter.PageHolder> {

    //推荐页（第 0 页）是竖向全屏流，其余三页是双列网格。
    //两个 viewType 必须分开：RecyclerView 只把 holder 回收给**同 viewType** 的位置，
    //所以分开之后，封面的那个 RecyclerView 永远不会被拿去当网格页用。
    private static final int FEED_PAGE = 0;
    private static final int TYPE_FEED = 0;
    private static final int TYPE_GRID = 1;

    private final List<List<VideoItem>> channelData;
    private final VideoAdapter.OnItemClickListener clickListener;

    //推荐页那一屏用的全屏流适配器。Activity 持有它、传进来，**不是每页 new 一个**：
    //它身上要存播放进度（第 4 步），而 ViewPager2 会销毁离屏页、连 RecyclerView
    //一起重建，状态只有放在这个长生命周期的实例上才活得过一次销毁。
    private final VideoFeedAdapter feedAdapter;

    //网格页的内边距（px）。上下两个值是 VideoActivity 量完 chrome 和
    //底部导航之后塞进来的，见 setGridPadding()
    private int padTop;
    private int padBottom;
    private int padHorizontal;

    public VideoChannelAdapter(List<List<VideoItem>> channelData,
                               VideoAdapter.OnItemClickListener clickListener,
                               VideoFeedAdapter feedAdapter) {
        this.channelData = channelData;
        this.clickListener = clickListener;
        this.feedAdapter = feedAdapter;
    }

    /**
     * 把量好的 chrome / 底部导航高度交给网格页当内边距。
     *
     * ⚠️ 幂等：三个值都没变就直接返回。VideoActivity 现在**每遍 layout 都
     *    重量一次**（理由见那边 measureChrome() 的注释），而下面这段是
     *    「重绑 4 页 + 每页滚回顶部」——重复执行的代价不只是白干，
     *    用户滚到一半的时候列表会被弹回顶部。
     *
     * ⚠️ 值真的变了就必须 notifyDataSetChanged()：setPadding 本身只会让已经
     *    绑定过的页重新布局，而此刻在 ViewPager2 缓存里的页可能还没绑过。
     *    整页重绑的开销是 4 页 × 5 条，可以忽略。
     */
    public void setGridPadding(int top, int bottom, int horizontal) {
        if (this.padTop == top && this.padBottom == bottom && this.padHorizontal == horizontal) {
            return;
        }
        this.padTop = top;
        this.padBottom = bottom;
        this.padHorizontal = horizontal;

        //同一个 bottom 还要转给全屏流：那边不能用 RV 的 padding（会破坏吸附），
        //只能做成 item 根节点的 paddingBottom，所以由它自己收。
        //「底部导航有多高」这件事只有一个来源，别在两个地方各量一次。
        //放在 notifyDataSetChanged() 之前：先让全屏流知道该留多少白，
        //再触发重绑，免得第一遍绑出来的是没有内边距的版本。
        feedAdapter.setItemPaddingBottom(bottom);

        notifyDataSetChanged();

        //⚠️ 光 notifyDataSetChanged() 不够，必须再把每一页滚回顶部。
        //   实测（child0.top 对比 paddingTop）：重绑之后列表会自己往上
        //   偏 106px，第一行封面正好被频道标签压住一截。
        //
        //   原因是内边距**只能在第一次 layout 之后**才知道（顶栏高度取决于
        //   状态栏 inset）：所以列表已经按 paddingTop=0 排过一遍了，再把
        //   padding 改成 322 重排，RecyclerView 沿用了上一遍的锚点偏移，
        //   没有把内容重新贴到新的内边距下沿。
        //
        //   post() 不能省：要等这次带新 padding 的重排走完再归零，
        //   在同一个消息里直接调会被随后的 layout 覆盖掉。
        for (RecyclerView rv : boundPages) {
            if (rv != null) {
                rv.post(() -> rv.scrollToPosition(0));
            }
        }
    }

    //已经绑定过的页，用来在改完内边距之后把滚动位置归零
    private final List<RecyclerView> boundPages = new ArrayList<>();

    static class PageHolder extends RecyclerView.ViewHolder {
        final RecyclerView rv;

        //这个 holder 当前画的是第几个频道。见 onBindViewHolder
        int boundChannel = -1;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            rv = (RecyclerView) itemView;
        }
    }

    @NonNull
    @Override
    public int getItemViewType(int position) {
        return position == FEED_PAGE ? TYPE_FEED : TYPE_GRID;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        boolean feed = viewType == TYPE_FEED;
        View view = LayoutInflater.from(parent.getContext()).inflate(
                feed ? R.layout.item_video_page_feed : R.layout.item_video_page_grid,
                parent, false);
        PageHolder holder = new PageHolder(view);

        if (feed) {
            //全屏流自己装 LinearLayoutManager + PagerSnapHelper
            //（在 onAttachedToRecyclerView 里，见 VideoFeedAdapter）。
            //⚠️ 这里**不能**设 GridLayoutManager，设了就不是一屏一个了。
            holder.rv.setAdapter(feedAdapter);
        } else {
            //双列等高网格，和改造前 activity_video.xml 里那套一致
            holder.rv.setLayoutManager(new GridLayoutManager(parent.getContext(), 2));
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        //推荐页整页由 VideoFeedAdapter 自己画，这里什么都不做。
        //⚠️ 尤其是**不能**调 rv.setPadding()：全屏流的 item 高度必须正好
        //   等于 RV 高度，RV 一有竖向 padding，PagerSnapHelper 的中点吸附
        //   就会在每次松手后露出相邻页的一条边。留白由 item 自己管。
        if (position == FEED_PAGE) return;

        //⚠️ 判 boundChannel 不能省。ViewPager2 底层还是 RecyclerView，
        //   横滑翻页时它会把上一页的 holder 回收给下一页用。少了这个判断，
        //   新页会顶着一张旧 adapter，内容串成隔壁频道。
        if (holder.boundChannel != position) {
            holder.boundChannel = position;
            VideoAdapter adapter = new VideoAdapter(channelData.get(position));
            adapter.setOnItemClickListener(clickListener);
            holder.rv.setAdapter(adapter);
        }
        while (boundPages.size() <= position) boundPages.add(null);
        boundPages.set(position, holder.rv);

        //⚠️ 用绝对值 setPadding，不要写 +=。这个回调会随重绑反复触发。
        //   左右两边的 space_m 也不是随手写的——必须和 item_video.xml 里
        //   那 4dp 的 layout_marginHorizontal 相加等于 page_padding(16dp)：
        //   GridLayoutManager 只平分「父容器去掉 padding 之后」的宽度，
        //   想让它算出来的第一列刚好和上面的频道标签对齐，只能拆成两段。
        //   把这里直接改成 16dp 会让第一列右移 4dp、右列也被挤窄 8dp。
        holder.rv.setPadding(padHorizontal, padTop, padHorizontal, padBottom);
    }


    @Override
    public int getItemCount() {
        return channelData.size();
    }
}
