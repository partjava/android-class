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
 */
public class VideoItem {
    private final String title;     // 视频标题
    private final String source;    // 来源号，如"央视新闻"
    private final String duration;  // 时长 "03:24"，显示在封面右下角
    private final String playCount; // 播放量 "12万次播放"
    private final String desc;      // 简介，播放页用
    private final int coverRes;     // 封面资源 id

    public VideoItem(String title, String source, String duration,
                     String playCount, String desc, int coverRes) {
        this.title = title;
        this.source = source;
        this.duration = duration;
        this.playCount = playCount;
        this.desc = desc;
        this.coverRes = coverRes;
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
}
