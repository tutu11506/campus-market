package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.UserCoupon;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

public interface CouponMapper {

    /** 待领取的券礼包（首次进入首页时弹窗展示） */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = 'pending' " +
            "AND expire_time > NOW() ORDER BY threshold_amount ASC")
    List<UserCoupon> findPendingList(@Param("userId") Integer userId);

    /** 已领取、未使用、且满足门槛的最优券（订单可用） */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = 'unused' " +
            "AND expire_time > NOW() AND threshold_amount <= #{amount} " +
            "ORDER BY discount_amount DESC, id ASC LIMIT 1")
    UserCoupon findApplicable(@Param("userId") Integer userId, @Param("amount") BigDecimal amount);

    /** 已领取的可用券 */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = 'unused' " +
            "AND expire_time > NOW() ORDER BY threshold_amount ASC")
    List<UserCoupon> findUnused(@Param("userId") Integer userId);

    /** 是否已有可用券（用于判断要不要补发） */
    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} " +
            "AND status IN ('pending', 'unused') AND expire_time > NOW()")
    int countAvailable(@Param("userId") Integer userId);

    @Insert("INSERT INTO user_coupon(user_id, threshold_amount, discount_amount, status, expire_time) " +
            "VALUES(#{userId}, #{thresholdAmount}, #{discountAmount}, #{status}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserCoupon coupon);

    /** 一键领取：pending → unused */
    @Update("UPDATE user_coupon SET status = 'unused' WHERE user_id = #{userId} " +
            "AND status = 'pending' AND expire_time > NOW()")
    int claimAll(@Param("userId") Integer userId);

    /** 使用：unused → used */
    @Update("UPDATE user_coupon SET status = 'used', order_id = #{orderId}, used_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 'unused'")
    int markUsed(@Param("id") Integer id, @Param("userId") Integer userId, @Param("orderId") Integer orderId);
}
