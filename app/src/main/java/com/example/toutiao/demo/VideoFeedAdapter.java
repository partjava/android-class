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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 「推荐」频道的全屏流适配器：一屏一个视频，上下滑切换，单击就地播放/暂停。
 *
 * 和 VideoAdapter（网格卡片）完全是两回事，所以是两个类：
 * 那边一个 item 是一张 160dp 宽的卡片、一次 bind 十几行、点击跳播放页；
 * 这边一个 item 是一整屏、一屏只有一条数据、点击在本屏内播放。
 * 硬塞进一个 adapter 只会得到一个到处是 if 的 onBindViewHolder。
 *
 * · 「一屏一个」靠 LinearLayoutManager(VERTICAL) + PagerSnapHelper
 *   ——和商城页宫格那个 PagerSnapHelper 是同一个东西，只是方向从
 *   横向换成竖向（PagerSnapHelper 本身与方向无关，一行都不用改）。
 * · 模拟播放的参数和语义与 VideoDetailActivity 完全一致
 *   （200ms 一跳、每跳 2%、走满停住不循环），只是那一份绑在三个具体的
 *   控件 id 上、这一份绑在列表位置上。两边各留一行交叉引用，不抽公共类。
 *
 * ⚠️ onAttachedToRecyclerView 会**反复触发**：ViewPager2 默认
 *    OFFSCREEN_PAGE_LIMIT_DEFAULT，超出缓存范围的页会被 destroy，
 *    再滑回来时整页连同 RecyclerView、ViewHolder 一起重建。
 *    所以每次 attach 都要重新装 LayoutManager / SnapHelper / 滚动监听，
 *    但又不能每 attach 一次就多挂一份（会重复响应滚动、进度越走越快）。
 *    这里的办法是记住当前 attach 的那个 RecyclerView：
 *    同一个就直接 return，换了新的才重装。
 *
 * ⚠️ 正因如此，**播放进度只能存在这个 adapter 里**，不能存在 holder 或 View 上
 *    ——它们会被重建。这个 adapter 是 VideoActivity 持有的长生命周期实例，
 *    活得过每一次页销毁。
 *
 * ⚠️ 进度条按 200ms 一跳刷新，刷新方式是**直接改当前那一屏的 View**，
 *    绝不 notifyItemChanged —— 那会把整屏重绑一遍（每次都 setImageResource），
 *    一秒五次。「全屏只有一屏可见」这个前提让直接改 View 是安全的。
 *
 * · 整屏的手势（bindGesture）：单击播放/暂停、双击点赞。
 *   单击用的是 onSingleTapConfirmed，所以有约 300ms 的判定窗口延迟，
 *   换来的是双击不会误切播放。
 * · 右侧操作栏（bindRail）：头像 / 点赞 / 评论 / 收藏 / 分享。
 *   **只有点赞是能改状态的**（点亮变红心、数字 +1、再点取消），
 *   状态存在 liked 集合里（也在 adapter 上，理由同上）；
 *   评论/收藏/分享点了弹 Toast。
 */
public class VideoFeedAdapter extends RecyclerView.Adapter<VideoFeedAdapter.FeedHolder> {

    //模拟播放：和 VideoDetailActivity 同一组参数，约 10 秒走满
    private static final long TICK_MS = 200L;
    private static final int TICK_STEP = 2;
    private static final int PROGRESS_MAX = 100;

    //双击那颗大红心的动画时长。600ms 是「看得见又不挡事」：
    //再长一点，连点几下会觉得那颗心赖着不走
    private static final long BURST_MS = 600L;

    private final List<VideoItem> data;

    //item 根节点的下内边距（px）。= 底部导航栏的实际高度，
    //由 VideoActivity.measureChrome() 量完塞进来，见 setItemPaddingBottom()
    private int padBottom;

    //当前挂着的 RecyclerView。用来判断这次 onAttachedToRecyclerView
    //是不是换了一个新的（见上面的 ⚠️）
    private RecyclerView attachedRv;
    //吸住的那一屏是不是「当前正在播的」那一屏。只在这一个实例上找，
    //所以字段是单份的，不是按位置存的数组
    private PagerSnapHelper snapHelper;

    //==== 播放状态（全部只属于 currentPos 这一条）====

    //当前吸附到的是第几条。上下滑换条时在这里更新
    private int currentPos;
    private int progress;
    private boolean playing;

    //推荐页是不是当前可见的频道页。左右滑到别的频道就变 false，
    //此时进度归零、定时器停掉；滑回来从 0 重播。
    //⚠️ 它和 playing 是两件事：playing 说的是「用户有没有按暂停」，
    //   active 说的是「这一页在不在屏幕上」。在别的频道时 active=false、
    //   playing 必然也是 false；但 active=true 时用户仍可以按暂停。
    private boolean active;

    /**
     * 已点赞的条目下标。
     *
     * ⚠️ 存在 adapter 上，和 currentPos / progress / playing 同一个理由：
     *    这个 adapter 是 VideoActivity 持有的长生命周期实例，活得过每一次
     *    「离屏页被销毁」。存在 holder 或 View 上的话，横滑去别的频道再
     *    滑回来，点赞就全没了。
     *
     * ⚠️ 只存**用户点出来的**这一下，不回写 VideoItem：
     *    数据里的 likeCount 是「初始点赞数」，显示出来的数字是
     *    「初始值 + 我赞没赞」，见 renderLike()。这样数据模型保持
     *    final、不需要 setter，也不用去动那 24 处 new VideoItem(...)。
     */
    private final Set<Integer> liked = new HashSet<>();

    private final Handler handler = new Handler(Looper.getMainLooper());

    //每 200ms 推进一次进度。用字段而不是匿名类，是为了能按同一个实例
    //removeCallbacks —— 每次 new 一个 Runnable 是移除不掉的
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            progress += TICK_STEP;
            if (progress >= PROGRESS_MAX) {
                progress = PROGRESS_MAX;
                renderProgress();
                //走满停在满格，不循环回 0
                pause();
                return;
            }
            renderProgress();
            handler.postDelayed(this, TICK_MS);
        }
    };

    public VideoFeedAdapter(List<VideoItem> data) {
        this.data = data;
    }

    /**
     * 设置每一屏的下内边距，把内容抬到底部导航栏之上。
     *
     * ⚠️ 是 item 根节点的 paddingBottom，**不是** RecyclerView 的 padding。
     *    原因见 item_video_page_feed.xml：RV 一旦有竖向 padding，
     *    item 就比 RV 矮，PagerSnapHelper 每次吸附都会露出相邻页的一条边。
     *    封面和进度条的下沿因此停在「底部导航上沿」，而不是屏幕最底。
     *
     * 幂等：重复调用同一个值直接返回。measureChrome() 可能被动重跑，
     * 每次都 notifyDataSetChanged() 会把正在看的这一屏顶回第一条。
     */
    public void setItemPaddingBottom(int px) {
        if (this.padBottom == px) return;
        this.padBottom = px;
        notifyDataSetChanged();

        //⚠️ 改内边距会重排，重排后吸附位置会飘。这里保留「当前看的是第几条」，
        //   不能 scrollToPosition(0)——用户可能已经滑到第 5 条了。
        //   post() 同样不能省：要等这次带新 padding 的重排走完再纠正。
        if (attachedRv != null) {
            attachedRv.post(() -> {
                RecyclerView.LayoutManager lm = attachedRv.getLayoutManager();
                if (!(lm instanceof LinearLayoutManager)) return;
                int first = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                if (first != RecyclerView.NO_POSITION) attachedRv.scrollToPosition(first);
            });
        }
    }

    //==== 给 VideoActivity 调的三个口子 ====

    /**
     * 推荐页成为 / 不再是当前可见的频道页。
     *
     * 由 VideoActivity 在 chrome 的**离散翻转点**上调用，和标签换配色同一个
     * 时机——不是 onPageSelected。理由是那个回调会被 ViewPager2 提前发出来
     * （早于动画）：点标签从推荐跳到小视频时，进度会在页面还没滑走时就归零，
     * 看着像闪了一下。挂在 t>=0.5 这个真正「谁占多数」的翻转点上，
     * 换进换出都只发生一次，而且和眼睛看到的一致。
     */
    public void setActive(boolean wantActive) {
        if (active == wantActive) return;
        active = wantActive;
        if (active) {
            //回到推荐页：从头播，不复用上次的进度（和播放页 onNewIntent 一致）
            progress = 0;
            play();
        } else {
            pause();
            progress = 0;
            renderProgress();
        }
    }

    /**
     * 宿主 Activity 走了（按底部导航去了别的页）。
     * 只停定时器、保留进度，回来能接着播。
     * ⚠️ 必须是 pause() 这条路，不能是 setActive(false)：active 还得是 true，
     *    否则回来时 onResume 不会重新开播。
     */
    public void onHostPause() {
        pause();
    }

    //宿主回来了。只有推荐页仍是当前页时才续播
    public void onHostResume() {
        if (active && !playing) play();
    }

    /**
     * 页面销毁。⚠️ 必须清干净：Handler 挂在主线程 Looper 上，
     * 漏掉的话被销毁的 adapter 还会被每 200ms 唤醒一次，
     * 而且在 Activity 已经没了之后去碰 attachedRv。
     */
    public void release() {
        handler.removeCallbacksAndMessages(null);
        playing = false;
    }

    //==== RecyclerView 装配 ====

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        if (attachedRv == rv) return;   //同一个 RV，不重装（见类注释）
        attachedRv = rv;

        //方向是 VERTICAL：抖音那种上下滑，不是商城宫格那种左右翻
        rv.setLayoutManager(new LinearLayoutManager(rv.getContext(),
                RecyclerView.VERTICAL, false));

        //每次 attach 都 new 一个新的：SnapHelper 没有 detach 的 API，
        //复用同一个实例会让它还挂在已经被销毁的那个 RV 上。
        //留着引用是为了在 onScrollStateChanged 里 findSnapView()
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rv);

        //⚠️ 判定「现在停在第几条」只能在 STATE_IDLE 里做。
        //   不能照抄商城页 updateEntryDots() 放进 onScrolled —— 那里是幂等的
        //   （只换个圆点背景），手势中途反复触发无所谓；这里一变就要
        //   复位旧条、开播新条，手指还拖着就会连翻好几轮。
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView r, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) syncToSnapView();
            }
        });
    }

    /**
     * 松手吸附完之后，把「当前是第几条」对齐到真正吸住的那一屏。
     *
     * 翻页中途经过的那几条不触发（只在 IDLE 里调），所以一次滑动最多换一次。
     */
    private void syncToSnapView() {
        if (attachedRv == null || snapHelper == null) return;
        RecyclerView.LayoutManager lm = attachedRv.getLayoutManager();
        View snapped = snapHelper.findSnapView(lm);
        if (snapped == null) return;
        int pos = lm.getPosition(snapped);
        if (pos == RecyclerView.NO_POSITION || pos == currentPos) return;

        int old = currentPos;
        currentPos = pos;
        //新的从 0 开始，旧的那条由 notifyItemChanged(old) 复位
        progress = 0;

        //⚠️ 用 notifyItemChanged 而不是 notifyDataSetChanged：前者只重绑这两屏，
        //   不动布局；后者会在吸附刚结束、scrollState 刚回到 IDLE 的这一帧
        //   重排整个列表，实测会把吸附位置顶偏。
        notifyItemChanged(old);
        notifyItemChanged(currentPos);

        //滑到新的一条：如果推荐页正显示着就接着播，否则保持停着
        if (active) play();
    }

    //==== 播放控制（语义对齐 VideoDetailActivity 的同名方法）====

    private void play() {
        //已经播完了，再开就从头开始
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

    //==== 渲染 ====

    private FeedHolder holderForCurrent() {
        if (attachedRv == null) return null;
        RecyclerView.ViewHolder vh =
                attachedRv.findViewHolderForAdapterPosition(currentPos);
        return (vh instanceof FeedHolder) ? (FeedHolder) vh : null;
    }

    /**
     * 把当前这一屏的进度画出来：直接改填充条的宽度。
     *
     * ⚠️ 不走 notifyItemChanged。这个方法是 200ms 一跳调用的，
     *    重绑整屏意味着每 200ms 重新 setImageResource 一次封面。
     */
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

    //百分比 → 像素宽度。和播放页一样按轨道实测宽度算
    private void applyProgress(FeedHolder h, int value) {
        int trackWidth = h.flProgress.getWidth();
        //还没测量完（宽度为 0）就先跳过，下一跳会补上
        if (trackWidth <= 0) return;
        ViewGroup.LayoutParams lp = h.vProgress.getLayoutParams();
        lp.width = trackWidth * value / PROGRESS_MAX;
        h.vProgress.setLayoutParams(lp);
    }

    //===== 点赞（操作栏里唯一会改状态的一项）=====

    /**
     * 把第 pos 条设成已赞 / 未赞，并把当前那一屏重画一遍。
     *
     * ⚠️ 绝不 notifyItemChanged：和进度条那条约定同一个理由——那会把整屏
     *    重绑一遍（每绑一次都要 setImageResource 封面）。「一屏只有一条可见」
     *    这个前提让直接改 View 是安全的。
     *
     * ⚠️ 状态没变就直接返回：双击已经赞过的那条时，wantLiked 传的是 true，
     *    此时只放动画、不动数字（抖音双击也不取消点赞）
     */
    private void like(FeedHolder holder, int pos, boolean wantLiked) {
        if (pos == RecyclerView.NO_POSITION) return;
        boolean changed = wantLiked ? liked.add(pos) : liked.remove(pos);
        if (!changed) return;
        renderLike(holder, pos);
    }

    /**
     * 画点赞态：心形换色 + 数字换文案。
     *
     * ⚠️ 换色用 tint，不换图片资源：点亮和未点亮是**同一个图形**
     *    （双击那颗大红心就是同一个 ic_like），差的就是一个颜色，
     *    而颜色在本项目一律走 tint。
     *
     * ⚠️ 未赞时必须显式设回白色。ic_like 自己的 fillColor 是 icon_primary
     *    （#333333），而 setImageTintList 一旦设过就会一直生效——
     *    不设回去的话，「取消点赞」之后那颗心会变成深灰色，
     *    压在任意封面上都看不清。
     *
     * ⚠️ 显示的数字是「初始点赞数 + 我赞没赞」，不改 VideoItem：
     *    数据模型保持 final，也不用去动那 24 处 new VideoItem(...)。
     */
    private void renderLike(FeedHolder holder, int position) {
        boolean isLiked = liked.contains(position);
        holder.ivLike.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                holder.itemView.getContext(), isLiked ? R.color.brand_red : R.color.on_brand)));
        holder.tvLike.setText(formatCount(
                data.get(position).getLikeCount() + (isLiked ? 1 : 0)));
    }

    /**
     * 双击那颗大红心：0.4 倍放大到 1 倍，同时淡出。
     *
     * ⚠️ 先 cancel() 再设属性：连点两下时，上一个动画可能还在跑，
     *    不取消的话「从 0.4 开始」这句话不成立，心会从半路接着缩放。
     */
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

    /**
     * 数字 → 显示文本：不上万就原样，上万换算成「x.x万」，
     * 和商城页「已售 x.x万件」是同一套展示习惯。
     *
     * 那边是 ShopAdapter.formatSales()（私有实例方法），没法直接拿来用；
     * 为这五行再开一个工具类也不划算——项目里两份模拟播放实现也是这么
     * 各留一份、两边注释互相交叉引用的。
     *
     * ⚠️ 数据里的点赞数刻意都取在几千这个量级：上万之后显示的是「1.3万」，
     *    用户点一下加一，文本不会变，看着像没反应。这不是 bug，是「按万取整」
     *    本身的代价，README 的已知遗留里记了一笔。
     */
    private static String formatCount(int count) {
        if (count >= 10000) {
            return String.format(Locale.CHINA, "%.1f万", count / 10000.0);
        }
        return String.valueOf(count);
    }

    static class FeedHolder extends RecyclerView.ViewHolder {
        final ImageView ivCover;
        final ImageView ivPlay;
        final TextView tvTitle;
        final TextView tvSource;
        final TextView tvDuration;
        final TextView tvPlay;
        final FrameLayout flProgress;
        final View vProgress;

        //右侧操作栏
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

        //整屏都是手势区（单击播放/暂停、双击点赞），不是只有中间那个 48dp 的键——
        //和沉浸播放页一致，点起来不用瞄准
        bindGesture(holder);
        //操作栏那几个入口在这里绑一次就够，理由见 bindRail()
        bindRail(holder);
        return holder;
    }

    /**
     * 整屏手势：单击播放/暂停、双击点赞。
     *
     * ⚠️ 单击必须走 onSingleTapConfirmed，不能用 onSingleTapUp。
     *    onSingleTapUp 在手指抬起那一刻就回调，那时还不知道这一下是不是
     *    双击的前半截——用它的话双击点赞会顺带把播放切两次（暂停又播上）。
     *    代价是每次单击都要等双击判定窗口（ViewConfiguration 的
     *    getDoubleTapTimeout()，标准值 300ms）过去才生效，手感上能察觉。
     *    这是抖音也有的取舍，README 的已知遗留里记了一笔。
     *
     * ⚠️ setOnTouchListener 必须**固定 return true**，这一条最关键：
     *    返回 false 的话 item 根节点不消费 DOWN，父容器直接接管后续事件，
     *    于是 UP（乃至全部后续事件）永远不会送到手势检测器里 ——
     *    表现为「单击完全没反应」，不报错、不崩溃，极难查。
     *    返回 true 之后：
     *      · 竖向滑动照旧：RecyclerView 是在 onInterceptTouchEvent 那一层
     *        把事件抢走的，跟子 View 消不消费 DOWN 无关；
     *      · 操作栏那五个子控件照旧能点：命中子控件时事件在子 View 就被
     *        消费掉了，根本走不到根节点这个监听器，所以点那颗心不会
     *        顺带切换播放/暂停。
     *
     * ⚠️ 原来是根节点的 setOnClickListener（整屏点击切播放），改成手势之后
     *    要一并删掉：留着的话单击会走「单击确认」和「点击」两条路，
     *    一次点击切两次播放，正好互相抵消，看着像没反应。
     */
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
                    //抖音的语义：双击**只赞不取消**，取消要去点右边那颗心
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

    /**
     * 操作栏的点击。和列表 item 一样在 onCreateViewHolder 里绑一次，
     * 回调里再现取 position —— 绑在 onBindViewHolder 里的话，
     * 每重绑一次就多挂一层监听器。
     */
    private void bindRail(FeedHolder holder) {
        final Context context = holder.itemView.getContext();

        //点赞那颗心是**开关**（再点取消），和双击那条路不对称，理由见 onDoubleTap
        holder.ivLike.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            like(holder, pos, !liked.contains(pos));
        });
        //⚠️ 另外四个都不改状态、只弹提示：它们是纯入口，没有对应的数据，
        //   摆个数字或者编一个状态出来反而像真的能点出什么
        holder.ivComment.setOnClickListener(v -> toast(context, "评论功能仅为演示"));
        holder.ivCollect.setOnClickListener(v -> toast(context, "收藏功能仅为演示"));
        holder.ivShare.setOnClickListener(v -> toast(context, "分享功能仅为演示"));
        holder.ivFollow.setOnClickListener(v -> toast(context, "关注功能仅为演示"));
        holder.tvAvatar.setOnClickListener(v -> toast(context, "作者主页仅为演示"));
    }

    private void toast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * 这一屏是不是当前正在播的那一条。手势只对当前屏生效——
     * 手指在别的屏上滑动时不该切换播放状态。
     *
     * ⚠️ getAdapterPosition()：RecyclerView 1.1.0 没有 getBindingAdapterPosition()
     *    （那是 1.2.0 才有的 API，javac 报「找不到符号」核过）。
     */
    private boolean isCurrent(FeedHolder holder) {
        int pos = holder.getAdapterPosition();
        return pos != RecyclerView.NO_POSITION && pos == currentPos;
    }

    @Override
    public void onBindViewHolder(@NonNull FeedHolder holder, int position) {
        VideoItem item = data.get(position);

        //绝对赋值的 setPadding，不能写 +=（重绑会反复触发）。
        //左右不设：封面要整屏铺满，信息区自己的 paddingHorizontal 管左右留白。
        holder.itemView.setPadding(0, 0, 0, padBottom);

        holder.ivCover.setImageResource(item.getCoverRes());
        holder.tvTitle.setText(item.getTitle());
        holder.tvSource.setText(item.getSource());
        holder.tvDuration.setText(item.getDuration());
        holder.tvPlay.setText(item.getPlayCount());

        //===== 右侧操作栏 =====
        //头像就是来源名的首字。数据里的 source 都非空，但 substring 前
        //还是判一下长度——空串会抛 StringIndexOutOfBounds，没必要为一行
        //假数据埋这个雷
        String source = item.getSource();
        holder.tvAvatar.setText(source.isEmpty() ? "视" : source.substring(0, 1));
        holder.tvComment.setText(formatCount(item.getCommentCount()));

        //⚠️ 点赞态按**位置**判，不是「谁最后绑过谁就亮」：holder 会在多条之间
        //   复用，少了这个判断，滑到新的一条会把上一条的红心带过来。
        //   和上面 ivPlay 那条是同一类坑
        renderLike(holder, position);

        //⚠️ 大红心必须复位。它是靠动画直接改 alpha/scale 的，holder 复用时
        //   不复位的话，滑到新的一条会先看见上一条那颗还没播完的心
        //   （连点两下的情况由 playLikeBurst() 自己 cancel，两处都写是有意的）
        holder.ivLikeBurst.animate().cancel();
        holder.ivLikeBurst.setAlpha(0f);
        holder.ivLikeBurst.setScaleX(1f);
        holder.ivLikeBurst.setScaleY(1f);

        //⚠️ 播不播是**按位置**判的，不是「谁最后绑过谁就播」：
        //   holder 会在多条之间复用，少了这个判断，滑到新的一条时会把
        //   上一条的暂停图标和进度条带过来。只有 currentPos 那一屏才是播放态。
        boolean isCurrent = position == currentPos;
        holder.ivPlay.setImageResource(isCurrent && playing
                ? R.drawable.ic_pause_circle : R.drawable.ic_play_circle);

        //⚠️ 宽度得等测量完再画，post 一次。position 传进去是为了防「post 还没跑、
        //   这一屏已经被重绑给别的条目」——那时候不该再按旧位置画。
        final int pos = position;
        final int value = isCurrent ? progress : 0;
        holder.flProgress.post(() -> {
            //getAdapterPosition() 在 holder 已被回收时返回 NO_POSITION（≠ pos），
            //所以这一句同时也是「这一屏还在不在」的判据
            if (holder.getAdapterPosition() == pos) applyProgress(holder, value);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
