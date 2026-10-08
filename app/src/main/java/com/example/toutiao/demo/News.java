package com.example.toutiao.demo;

public class News {
    // 5种新闻展示类型常量
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_SINGLE_IMG = 2;
    public static final int TYPE_THREE_IMG = 3;
    public static final int TYPE_VIDEO = 4;
    public static final int TYPE_BIG_IMG = 5;

    private String id;
    private String detailType;
    private String mediaUri;
    public String getId() { return id; }
    public String getDetailType() { return detailType; }
    public String getMediaUri() { return mediaUri; }
    public News withId(String id) { this.id = id; return this; }
    public News withDetail(String type, String uri) { detailType = type; mediaUri = uri; return this; }
    private int type;
    private String title;
    private String source;
    private String time;
    private int img1;
    private int img2;
    private int img3;
    private String content; //正文：仅用户发布的动态在内存里携带；站内文章正文统一在 NewsContentStore，详情页按需查询
    private String imageUrl; //远程封面 URL（接口拉回的文章才有）；本地文章为 null，走 img1 资源
    private String imageUrl2; //第二张远程配图 URL
    private String imageUrl3; //第三张远程配图 URL
    private String linkUrl; //原文网页链接
    private String blocksJson; // 真实正文与图片流 JSON

    public News withRemote(String imageUrl, String linkUrl) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        if (linkUrl != null && !linkUrl.trim().isEmpty()) {
            this.id = "news-url-" + java.util.UUID.nameUUIDFromBytes(linkUrl.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return this;
    }

    public News withRemote2(String imageUrl2) {
        this.imageUrl2 = imageUrl2;
        return this;
    }

    public News withRemote3(String imageUrl3) {
        this.imageUrl3 = imageUrl3;
        return this;
    }

    public String getImageUrl3() {
        return imageUrl3;
    }

    public News withBlocks(String blocksJson) {
        this.blocksJson = blocksJson;
        return this;
    }

    public String getBlocksJson() {
        return blocksJson;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImageUrl2() {
        return imageUrl2;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public News(int type, String title, String source, String time, int img1, int img2, int img3) {
        this.type = type;
        this.id = "news-" + java.util.UUID.nameUUIDFromBytes((source + "|" + title).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.detailType = type == TYPE_VIDEO ? NewsContract.VIDEO : NewsContract.HTML;
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
