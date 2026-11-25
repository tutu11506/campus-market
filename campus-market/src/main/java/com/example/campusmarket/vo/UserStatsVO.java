package com.example.campusmarket.vo;

/**
 * 个人中心数据概览
 */
public class UserStatsVO {

    private Integer published;   // 发布总数
    private Integer onSale;      // 在售
    private Integer sold;        // 已售
    private Integer favorites;   // 收藏
    private Integer reviews;     // 收到的评价

    public Integer getPublished() {
        return published;
    }

    public void setPublished(Integer published) {
        this.published = published;
    }

    public Integer getOnSale() {
        return onSale;
    }

    public void setOnSale(Integer onSale) {
        this.onSale = onSale;
    }

    public Integer getSold() {
        return sold;
    }

    public void setSold(Integer sold) {
        this.sold = sold;
    }

    public Integer getFavorites() {
        return favorites;
    }

    public void setFavorites(Integer favorites) {
        this.favorites = favorites;
    }

    public Integer getReviews() {
        return reviews;
    }

    public void setReviews(Integer reviews) {
        this.reviews = reviews;
    }
}
