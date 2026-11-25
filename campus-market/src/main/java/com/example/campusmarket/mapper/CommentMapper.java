package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CommentMapper {

    // 新增评价
    @Insert("INSERT INTO comment(order_id, goods_id, from_user_id, to_user_id, rating, content) " +
            "VALUES(#{orderId}, #{goodsId}, #{fromUserId}, #{toUserId}, #{rating}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    // 查询某个商品的所有评价
    @Select("SELECT c.*, u.username as from_user_name, u.avatar as from_user_avatar, g.title as goods_title, g.cover_image as goods_cover " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.from_user_id = u.id " +
            "LEFT JOIN goods g ON c.goods_id = g.id " +
            "WHERE c.goods_id = #{goodsId} " +
            "ORDER BY c.create_time DESC")
    List<Comment> findByGoodsId(Integer goodsId);

    // 查询我收到的评价
    @Select("SELECT c.*, u.username as from_user_name, u.avatar as from_user_avatar, g.title as goods_title, g.cover_image as goods_cover " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.from_user_id = u.id " +
            "LEFT JOIN goods g ON c.goods_id = g.id " +
            "WHERE c.to_user_id = #{userId} " +
            "ORDER BY c.create_time DESC")
    List<Comment> findByToUserId(Integer userId);

    // 分页查询某个商品的评价
    @Select("SELECT c.*, u.username as from_user_name, u.avatar as from_user_avatar, g.title as goods_title, g.cover_image as goods_cover " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.from_user_id = u.id " +
            "LEFT JOIN goods g ON c.goods_id = g.id " +
            "WHERE c.goods_id = #{goodsId} " +
            "ORDER BY c.create_time DESC LIMIT #{offset}, #{size}")
    List<Comment> findByGoodsIdPage(@Param("goodsId") Integer goodsId,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    // 统计某个商品的评价数
    @Select("SELECT COUNT(*) FROM comment WHERE goods_id = #{goodsId}")
    int countByGoodsId(@Param("goodsId") Integer goodsId);

    // 分页查询我收到的评价
    @Select("SELECT c.*, u.username as from_user_name, u.avatar as from_user_avatar, g.title as goods_title, g.cover_image as goods_cover " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.from_user_id = u.id " +
            "LEFT JOIN goods g ON c.goods_id = g.id " +
            "WHERE c.to_user_id = #{userId} " +
            "ORDER BY c.create_time DESC LIMIT #{offset}, #{size}")
    List<Comment> findByToUserIdPage(@Param("userId") Integer userId,
                                     @Param("offset") Integer offset,
                                     @Param("size") Integer size);

    // 统计我收到的评价数
    @Select("SELECT COUNT(*) FROM comment WHERE to_user_id = #{userId}")
    int countByToUserId(@Param("userId") Integer userId);

    // 查询订单是否已评价
    @Select("SELECT * FROM comment WHERE order_id = #{orderId}")
    Comment findByOrderId(Integer orderId);

    // 查询某人针对某订单的评价（双方互评时各自只能评一次）
    @Select("SELECT * FROM comment WHERE order_id = #{orderId} AND from_user_id = #{fromUserId} LIMIT 1")
    Comment findByOrderIdAndFromUserId(@Param("orderId") Integer orderId, @Param("fromUserId") Integer fromUserId);
}