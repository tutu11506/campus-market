package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Refund;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface RefundMapper {

    // 新建退货申请
    @Insert("INSERT INTO refund(refund_no, order_id, goods_id, buyer_id, seller_id, amount, reason, description, evidence_urls, status) " +
            "VALUES(#{refundNo}, #{orderId}, #{goodsId}, #{buyerId}, #{sellerId}, #{amount}, #{reason}, #{description}, #{evidenceUrls}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Refund refund);

    // 退货申请详情（关联订单/商品/买卖家信息）
    @Select("SELECT r.*, o.order_no AS order_no, g.title AS goods_title, g.cover_image AS goods_cover, " +
            "b.username AS buyer_name, b.avatar AS buyer_avatar, " +
            "s.username AS seller_name, s.avatar AS seller_avatar " +
            "FROM refund r " +
            "LEFT JOIN `order` o ON r.order_id = o.id " +
            "LEFT JOIN goods g ON r.goods_id = g.id " +
            "LEFT JOIN user b ON r.buyer_id = b.id " +
            "LEFT JOIN user s ON r.seller_id = s.id " +
            "WHERE r.id = #{id}")
    Refund findById(Integer id);

    // 某订单的退货申请列表（新→旧）
    @Select("SELECT r.*, o.order_no AS order_no, g.title AS goods_title, g.cover_image AS goods_cover, " +
            "b.username AS buyer_name, b.avatar AS buyer_avatar, " +
            "s.username AS seller_name, s.avatar AS seller_avatar " +
            "FROM refund r " +
            "LEFT JOIN `order` o ON r.order_id = o.id " +
            "LEFT JOIN goods g ON r.goods_id = g.id " +
            "LEFT JOIN user b ON r.buyer_id = b.id " +
            "LEFT JOIN user s ON r.seller_id = s.id " +
            "WHERE r.order_id = #{orderId} ORDER BY r.create_time DESC")
    List<Refund> findByOrderId(Integer orderId);

    // 某订单是否存在待审核的退货申请（防止重复申请）
    @Select("SELECT * FROM refund WHERE order_id = #{orderId} AND status = 'pending' LIMIT 1")
    Refund findPendingByOrderId(Integer orderId);

    // 卖家的退货申请列表（分页）
    @Select("SELECT r.*, o.order_no AS order_no, g.title AS goods_title, g.cover_image AS goods_cover, " +
            "b.username AS buyer_name, b.avatar AS buyer_avatar " +
            "FROM refund r " +
            "LEFT JOIN `order` o ON r.order_id = o.id " +
            "LEFT JOIN goods g ON r.goods_id = g.id " +
            "LEFT JOIN user b ON r.buyer_id = b.id " +
            "WHERE r.seller_id = #{sellerId} ORDER BY r.create_time DESC LIMIT #{offset}, #{size}")
    List<Refund> findSellerRefundsPage(@Param("sellerId") Integer sellerId,
                                       @Param("offset") Integer offset,
                                       @Param("size") Integer size);

    // 统计卖家的退货申请数
    @Select("SELECT COUNT(*) FROM refund WHERE seller_id = #{sellerId}")
    int countSellerRefunds(@Param("sellerId") Integer sellerId);

    // 审核：更新状态 + 审核人 + 审核时间 + 拒绝原因（同意时 rejectReason 传 null）
    @Update("UPDATE refund SET status = #{status}, auditor_id = #{auditorId}, audit_time = NOW(), " +
            "reject_reason = #{rejectReason} WHERE id = #{id}")
    int updateAudit(@Param("id") Integer id,
                    @Param("status") String status,
                    @Param("auditorId") Integer auditorId,
                    @Param("rejectReason") String rejectReason);
}
