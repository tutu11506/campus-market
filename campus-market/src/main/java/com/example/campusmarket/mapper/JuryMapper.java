package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.JuryReview;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface JuryMapper {

    String PAGE_SELECT =
            "SELECT r.*, o.order_no, o.price AS order_price, o.goods_id, " +
            "g.title AS goods_title, g.cover_image AS goods_cover, " +
            "ru.username AS reviewer_name, ru.avatar AS reviewer_avatar, " +
            "eu.username AS reviewee_name, eu.avatar AS reviewee_avatar, " +
            "(SELECT v.vote FROM jury_vote v WHERE v.review_id = r.id AND v.voter_id = #{userId}) AS my_vote " +
            "FROM jury_review r " +
            "LEFT JOIN `order` o ON r.order_id = o.id " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user ru ON r.reviewer_id = ru.id " +
            "LEFT JOIN user eu ON r.reviewee_id = eu.id ";

    @Insert("INSERT INTO jury_review(order_id, reviewer_id, reviewee_id, reviewer_role, rating, reason, evidence, status) " +
            "VALUES(#{orderId}, #{reviewerId}, #{revieweeId}, #{reviewerRole}, #{rating}, #{reason}, #{evidence}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JuryReview review);

    @Select("SELECT r.*, o.order_no, o.price AS order_price, g.title AS goods_title, g.cover_image AS goods_cover " +
            "FROM jury_review r LEFT JOIN `order` o ON r.order_id = o.id LEFT JOIN goods g ON o.goods_id = g.id " +
            "WHERE r.id = #{id}")
    JuryReview findById(Integer id);

    @Select("SELECT * FROM jury_review WHERE order_id = #{orderId} AND reviewer_id = #{reviewerId} LIMIT 1")
    JuryReview findByOrderAndReviewer(@Param("orderId") Integer orderId,
                                      @Param("reviewerId") Integer reviewerId);

    /* ==================== 列表分页 ==================== */

    @Select("<script>" + PAGE_SELECT +
            "<choose>" +
            "<when test='filter == \"mine\"'>WHERE (r.reviewer_id = #{userId} OR r.reviewee_id = #{userId}) </when>" +
            "<when test='filter == \"pending\"'>WHERE r.status = 'pending' AND r.reviewer_id != #{userId} AND r.reviewee_id != #{userId} </when>" +
            "<otherwise>WHERE r.status = 'displayed' </otherwise>" +
            "</choose>" +
            "ORDER BY r.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<JuryReview> findPage(@Param("filter") String filter,
                              @Param("userId") Integer userId,
                              @Param("limit") int limit,
                              @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM jury_review r " +
            "<choose>" +
            "<when test='filter == \"mine\"'>WHERE (r.reviewer_id = #{userId} OR r.reviewee_id = #{userId})</when>" +
            "<when test='filter == \"pending\"'>WHERE r.status = 'pending' AND r.reviewer_id != #{userId} AND r.reviewee_id != #{userId}</when>" +
            "<otherwise>WHERE r.status = 'displayed'</otherwise>" +
            "</choose>" +
            "</script>")
    int countPage(@Param("filter") String filter, @Param("userId") Integer userId);

    /* ==================== 可评价的已完成订单 ==================== */

    @Select("SELECT o.id AS orderId, o.order_no AS orderNo, o.price AS price, o.create_time AS createTime, " +
            "g.title AS goodsTitle, g.cover_image AS goodsCover, " +
            "CASE WHEN o.buyer_id = #{userId} THEN 'buyer' ELSE 'seller' END AS reviewerRole, " +
            "CASE WHEN o.buyer_id = #{userId} THEN o.seller_id ELSE o.buyer_id END AS revieweeId, " +
            "CASE WHEN o.buyer_id = #{userId} THEN s.username ELSE b.username END AS revieweeName, " +
            "CASE WHEN o.buyer_id = #{userId} THEN s.avatar ELSE b.avatar END AS revieweeAvatar " +
            "FROM `order` o " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "LEFT JOIN user b ON o.buyer_id = b.id " +
            "LEFT JOIN user s ON o.seller_id = s.id " +
            "WHERE o.status = 'completed' AND (o.buyer_id = #{userId} OR o.seller_id = #{userId}) " +
            "AND NOT EXISTS (SELECT 1 FROM jury_review r WHERE r.order_id = o.id AND r.reviewer_id = #{userId}) " +
            "ORDER BY o.id DESC LIMIT 50")
    List<Map<String, Object>> findReviewableOrders(Integer userId);

    @Select("SELECT COUNT(*) FROM `order` o WHERE o.status = 'completed' " +
            "AND (o.buyer_id = #{userId} OR o.seller_id = #{userId}) " +
            "AND NOT EXISTS (SELECT 1 FROM jury_review r WHERE r.order_id = o.id AND r.reviewer_id = #{userId})")
    int countReviewableOrders(Integer userId);

    /* ==================== 投票 ==================== */

    @Insert("INSERT INTO jury_vote(review_id, voter_id, vote) VALUES(#{reviewId}, #{voterId}, #{vote}) " +
            "ON DUPLICATE KEY UPDATE vote = VALUES(vote)")
    int upsertVote(@Param("reviewId") Integer reviewId,
                   @Param("voterId") Integer voterId,
                   @Param("vote") Integer vote);

    @Select("SELECT COUNT(*) FROM jury_vote WHERE review_id = #{reviewId} AND vote = 1")
    int countDisplayVotes(Integer reviewId);

    @Select("SELECT COUNT(*) FROM jury_vote WHERE review_id = #{reviewId} AND vote = 0")
    int countHideVotes(Integer reviewId);

    @Select("SELECT COUNT(*) FROM jury_vote WHERE review_id = #{reviewId}")
    int countVotes(Integer reviewId);

    @Update("UPDATE jury_review SET display_votes = #{displayVotes}, hide_votes = #{hideVotes}, " +
            "total_votes = #{totalVotes}, approve_rate = #{approveRate}, status = #{status} WHERE id = #{id}")
    int updateStats(@Param("id") Integer id,
                    @Param("displayVotes") int displayVotes,
                    @Param("hideVotes") int hideVotes,
                    @Param("totalVotes") int totalVotes,
                    @Param("approveRate") BigDecimal approveRate,
                    @Param("status") String status);

    /* ==================== 概览 ==================== */

    @Select("SELECT COUNT(*) FROM jury_review WHERE status = 'pending'")
    int countPending();

    @Select("SELECT COUNT(*) FROM jury_review WHERE status = 'displayed'")
    int countDisplayed();

    @Select("SELECT COUNT(*) FROM jury_review WHERE reviewer_id = #{userId}")
    int countMine(Integer userId);
}
