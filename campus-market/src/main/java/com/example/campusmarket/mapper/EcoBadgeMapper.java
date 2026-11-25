package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.EcoBadge;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface EcoBadgeMapper {

    @Select("SELECT * FROM eco_badge ORDER BY condition_type, condition_value")
    List<EcoBadge> listAll();

    @Select("SELECT * FROM eco_badge WHERE code = #{code} LIMIT 1")
    EcoBadge findByCode(String code);

    /** 我已解锁的勋章（按 condition_value 升序） */
    @Select("SELECT b.* FROM eco_badge b " +
            "INNER JOIN user_eco_badge ub ON ub.badge_id = b.id " +
            "WHERE ub.user_id = #{userId} " +
            "ORDER BY b.condition_type, b.condition_value DESC")
    List<EcoBadge> findByUser(Integer userId);

    @Select("SELECT COUNT(*) FROM user_eco_badge WHERE user_id = #{userId} AND badge_id = #{badgeId}")
    int countUserBadge(@Param("userId") Integer userId, @Param("badgeId") Integer badgeId);

    @Insert("INSERT INTO user_eco_badge(user_id, badge_id) VALUES(#{userId}, #{badgeId})")
    int grantBadge(@Param("userId") Integer userId, @Param("badgeId") Integer badgeId);

    /* ==================== 进度统计 ==================== */

    /** 用户累计捐赠次数 */
    @Select("SELECT COUNT(*) FROM donation_record WHERE user_id = #{userId} AND status = 'done'")
    int donationCount(Integer userId);

    /** 用户已成交商品（goods_sold） */
    @Select("SELECT COUNT(*) FROM `order` WHERE seller_id = #{userId} AND status = 'completed'")
    int goodsSoldCount(Integer userId);

    /** 用户完成的租赁订单数 */
    @Select("SELECT COUNT(*) FROM rental_order ro JOIN rental_item ri ON ri.id = ro.item_id " +
            "WHERE ri.owner_id = #{userId} AND ro.status = 'completed'")
    int rentalCompleteCount(Integer userId);

    /** 用户累计获得积分 */
    @Select("SELECT earned_total FROM point_account WHERE user_id = #{userId}")
    Integer earnedTotal(Integer userId);

    /** 学院勋章榜：user 表暂无 college 字段，暂聚合为全校 */
    @Select("SELECT '全部' AS college, COUNT(*) AS badge_count " +
            "FROM user_eco_badge ub")
    List<Map<String, Object>> collegeTopBadges(@Param("limit") int limit);
}
