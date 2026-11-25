package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Favorite;
import com.example.campusmarket.mapper.FavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private SubscribeService subscribeService;

    // 收藏/取消收藏
    public String toggle(Favorite favorite) {
        Favorite exist = favoriteMapper.checkFavorite(favorite.getUserId(), favorite.getGoodsId());
        if (exist != null) {
            // 已收藏，取消
            favoriteMapper.delete(favorite.getUserId(), favorite.getGoodsId());
            // 取消收藏后不再推送该商品降价提醒
            subscribeService.unwatchGoods(favorite.getUserId(), favorite.getGoodsId());
            return "已取消收藏";
        } else {
            // 未收藏，添加
            favoriteMapper.insert(favorite);
            // 收藏即关注，改价时自动对比提醒
            subscribeService.watchGoods(favorite.getUserId(), favorite.getGoodsId());
            return "收藏成功，降价会第一时间提醒你";
        }
    }

    // 分页查询我的收藏
    public PageResult<Favorite> findMyFavorites(Integer userId, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Favorite> list = favoriteMapper.findByUserIdPage(userId, p[2], p[1]);
        int total = favoriteMapper.countByUserId(userId);
        return PageResult.of(list, total, p[0], p[1]);
    }

    // 判断是否已收藏
    public boolean isFavorited(Integer userId, Integer goodsId) {
        return favoriteMapper.checkFavorite(userId, goodsId) != null;
    }
}