package com.example.toutiao.demo;

public class News {
    // 4种类型常量
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_SINGLE_IMG = 2;
    public static final int TYPE_THREE_IMG = 3;
    public static final int TYPE_VIDEO = 4;

    private int type;
    private String title;
    private String source;
    private String time;
    private int img1;
    private int img2;
    private int img3;
    private String content; //正文：仅用户发布的动态在内存里携带；站内文章正文统一在 NewsContentStore，详情页按需查询
    private String imageUrl; //远程封面 URL（接口拉回的文章才有）；本地文章为 null，走 img1 资源
    private String linkUrl; //原文网页链接（接口文章才有），详情页「阅读原文」用

    public News withRemote(String imageUrl, String linkUrl) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        return this;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public News(int type, String title, String source, String time, int img1, int img2, int img3) {
        this.type = type;
        this.title = title;
        this.source = source;
        this.time = time;
        this.img1 = img1;
        this.img2 = img2;
        this.img3 = img3;
    }

    //链式设置正文。站内文章不再走这里（正文在 NewsContentStore 按需查询），
    //只有发布页的用户动态还在用
    public News withContent(String content) {
        this.content = content;
        return this;
    }

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getTime() {
        return time;
    }

    public int getImg1() {
        return img1;
    }

    public int getImg2() {
        return img2;
    }

    public int getImg3() {
        return img3;
    }
}
