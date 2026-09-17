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
    private String content; //详情页正文

    public News(int type, String title, String source, String time, int img1, int img2, int img3) {
        this.type = type;
        this.title = title;
        this.source = source;
        this.time = time;
        this.img1 = img1;
        this.img2 = img2;
        this.img3 = img3;
    }

    //链式设置正文，构造数据时可以直接跟在 new News(...) 后面
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
