package com.example.toutiao.demo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 频道实体模型。
 * 对应首页顶端可滑动的频道标签，支持自由添加、排序与删除管理。
 */
public class Channel implements Serializable {
    private String id;
    private String name;
    private boolean isFixed; // 是否固定在“我的频道”中不可删除（如“推荐”、“校园要闻”）

    public Channel(String id, String name, boolean isFixed) {
        this.id = id;
        this.name = name;
        this.isFixed = isFixed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFixed() {
        return isFixed;
    }

    public void setFixed(boolean fixed) {
        isFixed = fixed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return Objects.equals(id, channel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
