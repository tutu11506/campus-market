package com.example.campusmarket.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 碳足迹数据来源：复用已完成订单（买家 + 卖家两侧都算一次环保贡献），零新表。
 */
@Mapper
public interface CarbonMapper {

    /**
     * 我在指定时间范围内参与完成的订单（买或卖），带商品分类用于估算重量。
     */
    @Select("SELECT o.id, o.price, c.name AS categoryName, " +
            "COALESCE(o.settle_time, o.pay_time, o.create_time) AS happenTime " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE o.status = 'completed' AND (o.buyer_id = #{userId} OR o.seller_id = #{userId}) " +
            "AND COALESCE(o.settle_time, o.pay_time, o.create_time) >= #{start} " +
            "AND COALESCE(o.settle_time, o.pay_time, o.create_time) < #{end} " +
            "ORDER BY happenTime DESC")
    List<Map<String, Object>> findOrders(@Param("userId") Integer userId,
                                         @Param("start") String start,
                                         @Param("end") String end);

    /**
     * 我全部的已完成订单（用于累计碳足迹与可选年份）。
     */
    @Select("SELECT o.id, o.price, c.name AS categoryName, " +
            "COALESCE(o.settle_time, o.pay_time, o.create_time) AS happenTime " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE o.status = 'completed' AND (o.buyer_id = #{userId} OR o.seller_id = #{userId}) " +
            "ORDER BY happenTime DESC")
    List<Map<String, Object>> findAllOrders(@Param("userId") Integer userId);
}
