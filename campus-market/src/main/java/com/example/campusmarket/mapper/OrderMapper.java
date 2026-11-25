package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Order;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderMapper {

    // 创建订单
    @Insert("INSERT INTO `order`(order_no, goods_id, buyer_id, seller_id, price, discount_amount, status, remark) " +
            "VALUES(#{orderNo}, #{goodsId}, #{buyerId}, #{sellerId}, #{price}, #{discountAmount}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    // 根据ID查询订单详情
    @Select("SELECT o.*, g.title as goods_title, g.cover_image as goods_cover, " +
            "b.username as buyer_name, b.avatar as buyer_avatar, " +
            "s.username as seller_name, s.avatar as seller_avatar, " +
            "CASE WHEN EXISTS (SELECT 1 FROM comment cm WHERE cm.order_id = o.id) THEN 1 ELSE 0 END AS is_commented " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user b ON o.buyer_id = b.id " +
            "LEFT JOIN user s ON o.seller_id = s.id " +
            "WHERE o.id = #{id}")
    Order findById(Integer id);

    // 查询我买到的（作为买家）
    @Select("SELECT o.*, g.title as goods_title, g.cover_image as goods_cover, " +
            "s.username as seller_name, s.avatar as seller_avatar, " +
            "CASE WHEN EXISTS (SELECT 1 FROM comment cm WHERE cm.order_id = o.id) THEN 1 ELSE 0 END AS is_commented " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user s ON o.seller_id = s.id " +
            "WHERE o.buyer_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    List<Order> findBuyerOrders(Integer userId);

    // 查询我卖出的（作为卖家）
    @Select("SELECT o.*, g.title as goods_title, g.cover_image as goods_cover, " +
            "b.username as buyer_name, b.avatar as buyer_avatar, " +
            "CASE WHEN EXISTS (SELECT 1 FROM comment cm WHERE cm.order_id = o.id) THEN 1 ELSE 0 END AS is_commented " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user b ON o.buyer_id = b.id " +
            "WHERE o.seller_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    List<Order> findSellerOrders(Integer userId);

    // 分页查询我买到的（作为买家）；isCommented 表示“我”是否已评价，支持买卖双方互评
    @Select("SELECT o.*, g.title as goods_title, g.cover_image as goods_cover, " +
            "s.username as seller_name, s.avatar as seller_avatar, " +
            "CASE WHEN EXISTS (SELECT 1 FROM comment cm WHERE cm.order_id = o.id AND cm.from_user_id = #{userId}) THEN 1 ELSE 0 END AS is_commented " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user s ON o.seller_id = s.id " +
            "WHERE o.buyer_id = #{userId} " +
            "ORDER BY o.create_time DESC LIMIT #{offset}, #{size}")
    List<Order> findBuyerOrdersPage(@Param("userId") Integer userId,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    // 统计我买到的订单数
    @Select("SELECT COUNT(*) FROM `order` WHERE buyer_id = #{userId}")
    int countBuyerOrders(@Param("userId") Integer userId);

    // 分页查询我卖出的（作为卖家）；isCommented 表示“我”是否已评价，支持买卖双方互评
    @Select("SELECT o.*, g.title as goods_title, g.cover_image as goods_cover, " +
            "b.username as buyer_name, b.avatar as buyer_avatar, " +
            "CASE WHEN EXISTS (SELECT 1 FROM comment cm WHERE cm.order_id = o.id AND cm.from_user_id = #{userId}) THEN 1 ELSE 0 END AS is_commented " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user b ON o.buyer_id = b.id " +
            "WHERE o.seller_id = #{userId} " +
            "ORDER BY o.create_time DESC LIMIT #{offset}, #{size}")
    List<Order> findSellerOrdersPage(@Param("userId") Integer userId,
                                     @Param("offset") Integer offset,
                                     @Param("size") Integer size);

    // 统计我卖出的订单数
    @Select("SELECT COUNT(*) FROM `order` WHERE seller_id = #{userId}")
    int countSellerOrders(@Param("userId") Integer userId);

    // 查询某买家对某商品是否存在进行中的订单（防止重复下单）
    @Select("SELECT * FROM `order` WHERE goods_id = #{goodsId} AND buyer_id = #{buyerId} AND status IN ('pending','dealing') LIMIT 1")
    Order findActiveByGoodsAndBuyer(@Param("goodsId") Integer goodsId, @Param("buyerId") Integer buyerId);

    // 修改订单状态
    @Update("UPDATE `order` SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    // 模拟支付：仅买家、待确认且未支付的订单可支付；支付后资金进入平台托管并生成核销码
    @Update("UPDATE `order` SET pay_time = NOW(), escrow_status = 'held', verify_code = #{verifyCode} " +
            "WHERE id = #{id} AND buyer_id = #{buyerId} AND status = 'pending' AND pay_time IS NULL")
    int payOrder(@Param("id") Integer id,
                 @Param("buyerId") Integer buyerId,
                 @Param("verifyCode") String verifyCode);

    // 资金结算给卖家（仅托管中的订单）
    @Update("UPDATE `order` SET escrow_status = 'settled', settle_time = NOW(), verify_time = NOW() " +
            "WHERE id = #{id} AND escrow_status = 'held'")
    int settleOrder(@Param("id") Integer id);

    // 托管资金退回买家（取消 / 退货）
    @Update("UPDATE `order` SET escrow_status = 'refunded' " +
            "WHERE id = #{id} AND escrow_status IN ('held', 'settled')")
    int refundEscrow(@Param("id") Integer id);

    // 查询超时未接单的待确认订单（从支付时间起算，未支付的订单不参与超时取消）
    @Select("SELECT * FROM `order` WHERE status = 'pending' AND pay_time IS NOT NULL " +
            "AND pay_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) ORDER BY id ASC")
    List<Order> findTimeoutPendingOrders(@Param("minutes") int minutes);

    // 仅当订单仍为待确认时取消，避免与卖家接单并发冲突（同时退回托管资金）
    @Update("UPDATE `order` SET status = 'cancelled', escrow_status = CASE WHEN escrow_status = 'held' THEN 'refunded' ELSE escrow_status END WHERE id = #{id} AND status = 'pending'")
    int cancelIfPending(Integer id);

    // 修改订单退货进度
    @Update("UPDATE `order` SET refund_status = #{refundStatus} WHERE id = #{id}")
    int updateRefundStatus(@Param("id") Integer id, @Param("refundStatus") String refundStatus);

    // 退货同意：同时更新订单状态与退货进度（原子）
    @Update("UPDATE `order` SET status = #{status}, refund_status = #{refundStatus} WHERE id = #{id}")
    int updateStatusAndRefundStatus(@Param("id") Integer id,
                                    @Param("status") String status,
                                    @Param("refundStatus") String refundStatus);

    // 统计总交易额（已完成的订单）
    @Select("SELECT COALESCE(SUM(price), 0) FROM `order` WHERE status = 'completed'")
    BigDecimal sumTotalAmount();

    // 统计订单总数
    @Select("SELECT COUNT(*) FROM `order`")
    int countAll();

    // 近 7 天每日订单数与成交额（管理后台看板）
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS date, COUNT(*) AS count, " +
            "COALESCE(SUM(CASE WHEN status = 'completed' THEN price ELSE 0 END), 0) AS amount " +
            "FROM `order` WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%m-%d') ORDER BY DATE_FORMAT(create_time, '%m-%d')")
    List<Map<String, Object>> countOrdersByDay();

    // 近 N 周（按周分组）订单数与成交额
    @Select("SELECT DATE_FORMAT(DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY), '%m-%d') AS date, " +
            "COUNT(*) AS count, " +
            "COALESCE(SUM(CASE WHEN status = 'completed' THEN price ELSE 0 END), 0) AS amount " +
            "FROM `order` WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL #{weeks} WEEK) " +
            "GROUP BY DATE_FORMAT(DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY), '%m-%d') " +
            "ORDER BY DATE_FORMAT(DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY), '%m-%d')")
    List<Map<String, Object>> countOrdersByWeek(@Param("weeks") int weeks);

    // 已完成订单数（成交率分子）
    @Select("SELECT COUNT(*) FROM `order` WHERE status = 'completed'")
    int countCompletedOrders();

    // 近 N 天有成交的活跃卖家数（distinct seller_id on completed orders）
    @Select("SELECT COUNT(DISTINCT seller_id) FROM `order` " +
            "WHERE status = 'completed' AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
    int countActiveSellersDays(@Param("days") int days);
}