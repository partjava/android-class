package com.example.toutiao.demo;

import java.util.Collections;
import java.util.List;

/** 视频专辑（专题）：一组按顺序播放的课程样片，可整体收藏。 */
public class VideoAlbum {
    private final String id;          // 稳定 id，如 "album-daguo"
    private final String title;       // 专辑标题
    private final String subtitle;    // 副标题，播放页/专辑页展示
    private final String updateInfo;  // 更新信息，如 "每周五更新"
    private final int coverRes;       // 封面资源 id
    private final List<String> episodeTitles; // 集标题（原始标题，有序）

    public VideoAlbum(String id, String title, String subtitle, String updateInfo,
                      int coverRes, List<String> episodeTitles) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.updateInfo = updateInfo;
        this.coverRes = coverRes;
        this.episodeTitles = episodeTitles == null
                ? Collections.<String>emptyList() : episodeTitles;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getUpdateInfo() { return updateInfo; }
    public int getCoverRes() { return coverRes; }
    public int getCount() { return episodeTitles.size(); }
    public List<String> getEpisodeTitles() { return episodeTitles; }
}
