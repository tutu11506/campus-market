package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Dispute;
import com.example.campusmarket.entity.DisputeEvidence;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DisputeMapper {

    String DETAIL_SELECT =
            "SELECT d.*, o.order_no, o.price AS order_price, o.status AS order_status, o.escrow_status AS order_escrow_status, " +
            "g.title AS goods_title, g.cover_image AS goods_cover, " +
            "iu.username AS initiator_name, iu.avatar AS initiator_avatar, " +
            "ru.username AS respondent_name, ru.avatar AS respondent_avatar, " +
            "(SELECT COUNT(*) FROM dispute_evidence e WHERE e.dispute_id = d.id) AS evidence_count " +
            "FROM dispute d " +
            "LEFT JOIN `order` o ON d.order_id = o.id " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user iu ON d.initiator_id = iu.id " +
            "LEFT JOIN user ru ON d.respondent_id = ru.id ";

    @Insert("INSERT INTO dispute(order_id, initiator_id, respondent_id, initiator_role, reason_type, description, claim_amount, status, escrow_frozen) " +
            "VALUES(#{orderId}, #{initiatorId}, #{respondentId}, #{initiatorRole}, #{reasonType}, #{description}, #{claimAmount}, #{status}, #{escrowFrozen})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Dispute dispute);

    @Select(DETAIL_SELECT + "WHERE d.id = #{id}")
    Dispute findById(Integer id);

    @Select("SELECT COUNT(*) FROM dispute WHERE order_id = #{orderId} AND status IN ('open', 'processing')")
    int countActiveByOrder(Integer orderId);

    @Select("<script>" + DETAIL_SELECT +
            "WHERE (d.initiator_id = #{userId} OR d.respondent_id = #{userId}) " +
            "<if test='status != null and status != \"\"'>AND d.status = #{status}</if> " +
            "ORDER BY d.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Dispute> findMyPage(@Param("userId") Integer userId,
                             @Param("status") String status,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM dispute WHERE (initiator_id = #{userId} OR respondent_id = #{userId}) " +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</script>")
    int countMy(@Param("userId") Integer userId, @Param("status") String status);

    @Select("<script>" + DETAIL_SELECT +
            "WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'>AND d.status = #{status}</if> " +
            "ORDER BY d.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Dispute> findAdminPage(@Param("status") String status,
                                @Param("limit") int limit,
                                @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM dispute WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</script>")
    int countAdmin(@Param("status") String status);

    @Update("UPDATE dispute SET escrow_frozen = 1 WHERE id = #{id}")
    int markFrozen(Integer id);

    @Update("UPDATE dispute SET status = 'processing', admin_id = #{adminId} WHERE id = #{id} AND status = 'open'")
    int markProcessing(@Param("id") Integer id, @Param("adminId") Integer adminId);

    @Update("UPDATE dispute SET status = 'resolved', verdict = #{verdict}, verdict_note = #{note}, admin_id = #{adminId}, " +
            "resolved_at = NOW() WHERE id = #{id} AND status IN ('open', 'processing')")
    int resolve(@Param("id") Integer id,
                @Param("verdict") String verdict,
                @Param("note") String note,
                @Param("adminId") Integer adminId);

    /* ==================== 证据 ==================== */

    @Insert("INSERT INTO dispute_evidence(dispute_id, uploader_id, type, content, file_url) " +
            "VALUES(#{disputeId}, #{uploaderId}, #{type}, #{content}, #{fileUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEvidence(DisputeEvidence evidence);

    @Select("SELECT e.*, u.username AS uploader_name, u.avatar AS uploader_avatar " +
            "FROM dispute_evidence e LEFT JOIN user u ON e.uploader_id = u.id " +
            "WHERE e.dispute_id = #{disputeId} ORDER BY e.id ASC")
    List<DisputeEvidence> findEvidence(Integer disputeId);

    /* ==================== 订单担保资金联动 ==================== */

    /** 冻结托管中的担保资金 */
    @Update("UPDATE `order` SET escrow_status = 'frozen' WHERE id = #{orderId} AND escrow_status = 'held'")
    int freezeEscrow(Integer orderId);

    /** 退款给买家（托管中或已冻结均可） */
    @Update("UPDATE `order` SET escrow_status = 'refunded' WHERE id = #{orderId} AND escrow_status IN ('held', 'frozen')")
    int refundEscrow(Integer orderId);

    /** 结算给卖家（托管中或已冻结均可） */
    @Update("UPDATE `order` SET escrow_status = 'settled', settle_time = NOW() " +
            "WHERE id = #{orderId} AND escrow_status IN ('held', 'frozen')")
    int settleEscrow(Integer orderId);

    @Update("UPDATE `order` SET status = #{status} WHERE id = #{id}")
    int updateOrderStatus(@Param("id") Integer id, @Param("status") String status);

    @Update("UPDATE `order` SET status = #{status}, refund_status = #{refundStatus} WHERE id = #{id}")
    int updateOrderStatusAndRefund(@Param("id") Integer id,
                                   @Param("status") String status,
                                   @Param("refundStatus") String refundStatus);

    /* ==================== 概览统计 ==================== */

    @Select("SELECT COUNT(*) FROM dispute WHERE status IN ('open', 'processing')")
    int countPending();

    @Select("SELECT COUNT(*) FROM dispute")
    int countAll();
}
