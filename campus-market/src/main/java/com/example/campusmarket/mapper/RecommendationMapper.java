package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Goods;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 个性化推荐查询 Mapper。
 * 支撑三类推荐场景：
 *   1) 配件/互补品推荐 —— 买了数码 → 推同分类其他商品
 *   2) 时序新款推荐 —— 购买超过 N 个月 → 推同分类最新发布
 *   3) 浏览兴趣推荐 —— 浏览过的分类里找热门商品
 */
public interface RecommendationMapper {

    /**
     * 查询用户已完成订单涉及的商品分类及其购买次数、最近购买时间。
     * 仅统计已支付且已完成的订单（pay_time 不为空，status='completed'）。
     */
    @Select("SELECT g.category_id AS categoryId, " +
            "COUNT(o.id) AS buyCount, " +
            "MAX(o.pay_time) AS lastBuyTime " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "WHERE o.buyer_id = #{userId} " +
            "AND o.pay_time IS NOT NULL " +
            "AND o.status = 'completed' " +
            "AND g.category_id IS NOT NULL " +
            "GROUP BY g.category_id " +
            "ORDER BY buyCount DESC, lastBuyTime DESC")
    List<Map<String, Object>> findUserBuyCategories(@Param("userId") Integer userId);

    /**
     * 查询用户浏览历史涉及的分类及其浏览次数、最近浏览时间。
     */
    @Select("SELECT g.category_id AS categoryId, " +
            "COUNT(DISTINCT bh.goods_id) AS browseCount, " +
            "MAX(bh.create_time) AS lastBrowseTime " +
            "FROM browse_history bh " +
            "LEFT JOIN goods g ON bh.goods_id = g.id " +
            "WHERE bh.user_id = #{userId} " +
            "AND g.category_id IS NOT NULL " +
            "AND g.is_deleted = 0 " +
            "GROUP BY g.category_id " +
            "ORDER BY browseCount DESC, lastBrowseTime DESC")
    List<Map<String, Object>> findUserBrowseCategories(@Param("userId") Integer userId);

    /**
     * 查询用户已购买（完成订单）的 goods_id 集合，用于推荐时排除已拥有的商品。
     */
    @Select("SELECT DISTINCT o.goods_id FROM `order` o " +
            "WHERE o.buyer_id = #{userId} " +
            "AND o.pay_time IS NOT NULL " +
            "AND o.status IN ('completed', 'dealing')")
    List<Integer> findUserBoughtGoodsIds(@Param("userId") Integer userId);

    /**
     * 按分类列表推荐商品，排除指定商品 ID，按浏览量排序。
     * 用于「配件/互补品推荐」和「浏览兴趣推荐」。
     */
    @Select("<script>" +
            "SELECT g.*, u.username as seller_name, c.name as category_name, " +
            "(SELECT COUNT(*) FROM comment cm WHERE cm.goods_id = g.id) AS comment_count, " +
            "(SELECT ROUND(AVG(cm.rating), 1) FROM comment cm WHERE cm.goods_id = g.id) AS avg_rating " +
            "FROM goods g " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND g.category_id IN " +
            "<foreach collection='categoryIds' item='catId' open='(' separator=',' close=')'>" +
            "#{catId}" +
            "</foreach> " +
            "<if test='excludeIds != null and excludeIds.size() > 0'>" +
            "AND g.id NOT IN " +
            "<foreach collection='excludeIds' item='eid' open='(' separator=',' close=')'>" +
            "#{eid}" +
            "</foreach> " +
            "</if>" +
            "ORDER BY g.view_count DESC, g.create_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Goods> findGoodsByCategoriesExcluding(@Param("categoryIds") List<Integer> categoryIds,
                                               @Param("excludeIds") List<Integer> excludeIds,
                                               @Param("limit") int limit);

    /**
     * 时序新款推荐：给定购买时间阈值（如 6 个月前），推荐这些分类下**新发布**的商品。
     * 核心：商品 create_time 要晚于用户最近一次购买该分类商品的时间，且排除已购买商品。
     */
    @Select("<script>" +
            "SELECT g.*, u.username as seller_name, c.name as category_name, " +
            "(SELECT COUNT(*) FROM comment cm WHERE cm.goods_id = g.id) AS comment_count, " +
            "(SELECT ROUND(AVG(cm.rating), 1) FROM comment cm WHERE cm.goods_id = g.id) AS avg_rating " +
            "FROM goods g " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND g.category_id IN " +
            "<foreach collection='categoryIds' item='catId' open='(' separator=',' close=')'>" +
            "#{catId}" +
            "</foreach> " +
            "AND g.create_time &gt; DATE_SUB(NOW(), INTERVAL #{months} MONTH) " +
            "<if test='excludeIds != null and excludeIds.size() > 0'>" +
            "AND g.id NOT IN " +
            "<foreach collection='excludeIds' item='eid' open='(' separator=',' close=')'>" +
            "#{eid}" +
            "</foreach> " +
            "</if>" +
            "ORDER BY g.create_time DESC, g.view_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Goods> findNewArrivalsByCategories(@Param("categoryIds") List<Integer> categoryIds,
                                           @Param("excludeIds") List<Integer> excludeIds,
                                           @Param("months") int months,
                                           @Param("limit") int limit);

    /**
     * 全站热门兜底（未登录或无行为数据时使用）。
     */
    @Select("SELECT g.*, u.username as seller_name, c.name as category_name, " +
            "(SELECT COUNT(*) FROM comment cm WHERE cm.goods_id = g.id) AS comment_count, " +
            "(SELECT ROUND(AVG(cm.rating), 1) FROM comment cm WHERE cm.goods_id = g.id) AS avg_rating " +
            "FROM goods g " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "ORDER BY g.view_count DESC, g.create_time DESC " +
            "LIMIT #{limit}")
    List<Goods> findHotGoods(@Param("limit") int limit);

    // ================== ItemCF 协同过滤 ==================

    /**
     * 核心 ItemCF SQL：基于"共购用户"计算商品相似度。
     *
     * 逻辑：
     *   1) 找出当前用户所有已完成订单涉及的商品集合 U_goods
     *   2) 找出和当前用户买过**同一件商品**的其他买家（o1.buyer_id != o2.buyer_id）
     *   3) 这些"同购买家"还买过的**其他商品**，按共同买家数排序
     *
     * 例：用户买了"手机"和"笔记本"，有 3 个人也买了这两件，
     *     其中 2 个人还买了"手机壳" → 手机壳 co_buy_count=2 排在前面。
     *
     * 这就是亚马逊经典的 "Users who bought X also bought Y" 协同过滤。
     */
    @Select("<script>" +
            "SELECT g.*, u.username as seller_name, u.student_no as seller_student_no, " +
            "c.name as category_name, " +
            "(SELECT COUNT(*) FROM comment cm WHERE cm.goods_id = g.id) AS comment_count, " +
            "(SELECT ROUND(AVG(cm.rating), 1) FROM comment cm WHERE cm.goods_id = g.id) AS avg_rating, " +
            "COUNT(DISTINCT co_buyers.buyer_id) AS co_buy_count " +
            "FROM goods g " +
            "JOIN (" +
            "  SELECT DISTINCT o2.buyer_id, o3.goods_id " +
            "  FROM `order` o1 " +
            "  JOIN `order` o2 ON o1.goods_id = o2.goods_id AND o1.buyer_id != o2.buyer_id " +
            "  JOIN `order` o3 ON o2.buyer_id = o3.buyer_id AND o3.goods_id != o1.goods_id " +
            "  WHERE o1.buyer_id = #{userId}" +
            "    AND o1.status = 'completed' AND o1.pay_time IS NOT NULL" +
            "    AND o2.status = 'completed' AND o2.pay_time IS NOT NULL" +
            "    AND o3.status = 'completed' AND o3.pay_time IS NOT NULL" +
            ") co_buyers ON co_buyers.goods_id = g.id " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "<if test='excludeIds != null and excludeIds.size() > 0'>" +
            "AND g.id NOT IN " +
            "<foreach collection='excludeIds' item='eid' open='(' separator=',' close=')'>" +
            "#{eid}" +
            "</foreach> " +
            "</if>" +
            "GROUP BY g.id " +
            "ORDER BY co_buy_count DESC, g.view_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Goods> findCoPurchasedGoods(@Param("userId") Integer userId,
                                     @Param("excludeIds") List<Integer> excludeIds,
                                     @Param("limit") int limit);

    /**
     * 复购提醒 —— 找到用户在指定时间窗口之前买过的**消费品类商品**。
     * 消费品类：生活用品(3)、教材书籍(1)、运动户外(5) —— 这些东西会用完/需要换新。
     *
     * 返回字段里多了 lastBuyDate，前端可直接展示"你去年 3 月买过的..."。
     */
    @Select("<script>" +
            "SELECT o.goods_id AS goodsId, g.title, g.category_id AS categoryId, " +
            "c.name AS categoryName, o.pay_time AS lastBuyDate, " +
            "DATEDIFF(NOW(), o.pay_time) AS daysSinceBuy, " +
            "g.price, g.cover_image AS coverImage " +
            "FROM `order` o " +
            "JOIN goods g ON o.goods_id = g.id " +
            "JOIN category c ON g.category_id = c.id " +
            "WHERE o.buyer_id = #{userId} " +
            "AND o.status = 'completed' AND o.pay_time IS NOT NULL " +
            "AND g.category_id IN " +
            "<foreach collection='categoryIds' item='cid' open='(' separator=',' close=')'>" +
            "#{cid}" +
            "</foreach> " +
            "AND o.pay_time &lt; DATE_SUB(NOW(), INTERVAL #{daysAgo} DAY) " +
            "AND o.pay_time &gt; DATE_SUB(NOW(), INTERVAL #{maxDaysAgo} DAY) " +
            "ORDER BY o.pay_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> findRepurchaseReminders(@Param("userId") Integer userId,
                                                      @Param("categoryIds") List<Integer> categoryIds,
                                                      @Param("daysAgo") int daysAgo,
                                                      @Param("maxDaysAgo") int maxDaysAgo,
                                                      @Param("limit") int limit);
}
