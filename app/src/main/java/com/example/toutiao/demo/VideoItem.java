package com.example.toutiao.demo;

/**
 * 视频数据模型。
 *
 * 和 ShopItem 一样用 final 字段 + 全参构造：数据是硬编码的，不需要 setter。
 * 封面同样是本地 drawable，全项目"不联网、无后台"的原则保持一致。
 *
 * 没有 category 字段：视频页的 4 个频道各自持有一份完整列表
 * （见 VideoActivity.initVideoData()），切频道是整份换掉，
 * 不像商城页那样从 allItems 里按分类过滤。
 *
 * 播放量还是 String（"12万次播放"），点赞/评论数却是 int，这不是不统一：
 * 播放量只显示、不参与计算，作者想怎么写都行；点赞数要**加一**，
 * 所以必须是数字，显示时再由 VideoFeedAdapter.formatCount() 转成文本。
 * 商城页的「已售件数」也是这个分工（int + formatSales()）。
 *
 * 只有点赞和评论两项有计数，收藏和分享没有字段：
 * 全屏流右侧操作栏里，点赞和评论是要把数字摆出来的，另外两个是纯入口
 * （点了只弹提示），没有数字可摆，也就没必要在数据里编一个。
 */
public class VideoItem {
    private final String title;     // 视频标题
    private final String source;    // 来源号，如"央视新闻"
    private final String duration;  // 时长 "03:24"，显示在封面右下角
    private final String playCount; // 播放量 "12万次播放"
    private final String desc;      // 简介，播放页用
    private final int coverRes;     // 封面资源 id
    private final int likeCount;    // 点赞数，全屏流操作栏用（可 +1）
    private final int commentCount; // 评论数，全屏流操作栏用

    /**
     * ⚠️ 点赞数和评论数**追加在末尾**，不是插在中间。
     *    项目里有 24 处 new VideoItem(...) 的调用点（VideoActivity 20 处、
     *    VideoDetailActivity 4 处），追加的话每处只要在末尾补两个实参；
     *    插在中间就得把后面所有实参往后挪一遍，改错了编译器还不一定报。
     *
     * ⚠️ 两个计数都是 int，且相邻——写反了编译器不会报错，只能靠读一遍。
     *    调用点的写法是「..., R.drawable.xxx, 8600, 326)」，两个数字成对出现。
     */
    public VideoItem(String title, String source, String duration,
                     String playCount, String desc, int coverRes,
                     int likeCount, int commentCount) {
        this.title = title;
        this.source = source;
        this.duration = duration;
        this.playCount = playCount;
        this.desc = desc;
        this.coverRes = coverRes;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getDuration() {
        return duration;
    }

    public String getPlayCount() {
        return playCount;
    }

    public String getDesc() {
        return desc;
    }

    public int getCoverRes() {
        return coverRes;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }
}
