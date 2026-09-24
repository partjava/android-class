package com.example.toutiao.demo;

public class VideoItem {
    private final String title;     // 视频标题
    private final String source;    // 来源号，如"央视新闻"
    private final String duration;  // 时长 "03:24"，显示在封面右下角
    private final String playCount; // 播放量 "12万次播放"
    private final String desc;      // 简介，播放页用
    private final int coverRes;     // 封面资源 id
    private final int likeCount;    // 点赞数，全屏流操作栏用（可 +1）
    private final int commentCount; // 评论数，全屏流操作栏用

    
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

    public String getId() { return stableId(title); }
    public static String stableId(String title) { return java.util.UUID.nameUUIDFromBytes(title.replace("课程样片 · ", "").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(); }
    public int getVideoRes() { return sampleFor(title); }
    public static int sampleFor(String title) { int n=Math.floorMod(stableId(title).hashCode(),3);return n==0?R.raw.course_motion_1:n==1?R.raw.course_motion_2:R.raw.course_motion_3; }
    public String getTitle() {
        return title.startsWith("课程样片 · ") ? title : "课程样片 · " + title;
    }

    public String getSource() {
        return "离线影像课堂";
    }

    public String getDuration() {
        return "00:08";
    }

    public String getPlayCount() {
        return "本地样片";
    }

    public String getDesc() {
        return "本片为生成的8秒无声动画课程样片，不是标题所述新闻或事件的原视频。\n\n" + desc;
    }

    public int getCoverRes() {
        return coverRes;
    }

    public int getLikeCount() {
        return 0;
    }

    public int getCommentCount() {
        return 0;
    }
}
