package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Favorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FavoriteMapper {

    // 新增收藏
    @Insert("INSERT INTO favorite(user_id, goods_id) VALUES(#{userId}, #{goodsId})")
    int insert(Favorite favorite);

    // 取消收藏（多参数必须加 @Param，否则 MyBatis 无法映射）
    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND goods_id = #{goodsId}")
    int delete(@Param("userId") Integer userId, @Param("goodsId") Integer goodsId);

    // 查询我的收藏列表
    @Select("SELECT f.*, g.title as goods_title, g.cover_image as goods_cover, g.price as goods_price, " +
            "u.username as seller_name " +
            "FROM favorite f " +
            "LEFT JOIN goods g ON f.goods_id = g.id " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "WHERE f.user_id = #{userId} AND g.is_deleted = 0 " +
            "ORDER BY f.create_time DESC")
    List<Favorite> findByUserId(Integer userId);

    // 分页查询我的收藏列表
    @Select("SELECT f.*, g.title as goods_title, g.cover_image as goods_cover, g.price as goods_price, " +
            "u.username as seller_name " +
            "FROM favorite f " +
            "LEFT JOIN goods g ON f.goods_id = g.id " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "WHERE f.user_id = #{userId} AND g.is_deleted = 0 " +
            "ORDER BY f.create_time DESC LIMIT #{offset}, #{size}")
    List<Favorite> findByUserIdPage(@Param("userId") Integer userId,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    // 统计我的收藏数
    @Select("SELECT COUNT(*) FROM favorite f " +
            "LEFT JOIN goods g ON f.goods_id = g.id " +
            "WHERE f.user_id = #{userId} AND g.is_deleted = 0")
    int countByUserId(@Param("userId") Integer userId);

    // 判断是否已收藏
    @Select("SELECT * FROM favorite WHERE user_id = #{userId} AND goods_id = #{goodsId}")
    Favorite checkFavorite(Integer userId, Integer goodsId);
}