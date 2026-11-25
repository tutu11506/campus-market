package com.example.campusmarket.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 营收账单 / 商品曝光数据来源。
 * 全部复用现有表（order / campus_task / point_record / goods / favorite / message），零新表。
 */
@Mapper
public interface FinanceMapper {

    /* ==================== 营收账单 ==================== */

    /**
     * 卖出收入：我作为卖家的已完成订单，时间优先取结算时间（真实到账时间）。
     */
    @Select("SELECT o.id, o.order_no AS orderNo, o.price AS amount, " +
            "COALESCE(o.settle_time, o.pay_time, o.create_time) AS happenTime, " +
            "g.title AS title, u.username AS buyerName " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user u ON o.buyer_id = u.id " +
            "WHERE o.seller_id = #{userId} AND o.status = 'completed' " +
            "AND COALESCE(o.settle_time, o.pay_time, o.create_time) >= #{start} " +
            "AND COALESCE(o.settle_time, o.pay_time, o.create_time) < #{end} " +
            "ORDER BY happenTime DESC")
    List<Map<String, Object>> findIncome(@Param("userId") Integer userId,
                                         @Param("start") String start,
                                         @Param("end") String end);

    /**
     * 跑腿支出：我发布的已完成任务，奖励即支出，时间取确认完成时间。
     */
    @Select("SELECT t.id, t.title, t.reward AS amount, " +
            "COALESCE(t.complete_time, t.create_time) AS happenTime, " +
            "a.username AS accepterName " +
            "FROM campus_task t " +
            "LEFT JOIN user a ON t.accepter_id = a.id " +
            "WHERE t.publisher_id = #{userId} AND t.status = 'completed' AND t.is_deleted = 0 " +
            "AND COALESCE(t.complete_time, t.create_time) >= #{start} " +
            "AND COALESCE(t.complete_time, t.create_time) < #{end} " +
            "ORDER BY happenTime DESC")
    List<Map<String, Object>> findErrands(@Param("userId") Integer userId,
                                          @Param("start") String start,
                                          @Param("end") String end);

    /**
     * 积分变动：积分流水按月份过滤。
     */
    @Select("SELECT id, direction, points, balance_after AS balanceAfter, type, remark, " +
            "created_at AS happenTime " +
            "FROM point_record " +
            "WHERE user_id = #{userId} AND created_at >= #{start} AND created_at < #{end} " +
            "ORDER BY created_at DESC")
    List<Map<String, Object>> findPointRecords(@Param("userId") Integer userId,
                                               @Param("start") String start,
                                               @Param("end") String end);

    /* ==================== 商品曝光 ==================== */

    /**
     * 我的商品曝光明细：浏览量、收藏数、咨询人数（按咨询过的买家去重）。
     */
    @Select("SELECT g.id, g.title, g.cover_image AS coverImage, g.price, g.status, " +
            "g.view_count AS views, g.location, " +
            "(SELECT COUNT(*) FROM favorite f WHERE f.goods_id = g.id) AS favorites, " +
            "(SELECT COUNT(DISTINCT m.sender_id) FROM message m " +
            " WHERE m.goods_id = g.id AND m.sender_id <> g.seller_id) AS consults " +
            "FROM goods g " +
            "WHERE g.seller_id = #{userId} AND g.is_deleted = 0 " +
            "ORDER BY g.view_count DESC, g.create_time DESC")
    List<Map<String, Object>> findGoodsExposure(@Param("userId") Integer userId);
}
