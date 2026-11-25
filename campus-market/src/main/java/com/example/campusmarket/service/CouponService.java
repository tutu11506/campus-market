package com.example.campusmarket.service;

import com.example.campusmarket.entity.UserCoupon;
import com.example.campusmarket.mapper.CouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 平台满减券礼包：首次进入首页时一次性发放 5 张不同档位的券，
 * 全部领完则不再打扰；等 5 张全部用完之后，再发下一批。
 */
@Service
public class CouponService {

    /** 五档券：满 50 减 5 / 满 100 减 10 / 满 200 减 20 / 满 300 减 30 / 满 500 减 50 */
    private static final BigDecimal[][] TIERS = {
            {BigDecimal.valueOf(50), BigDecimal.valueOf(5)},
            {BigDecimal.valueOf(100), BigDecimal.valueOf(10)},
            {BigDecimal.valueOf(200), BigDecimal.valueOf(20)},
            {BigDecimal.valueOf(300), BigDecimal.valueOf(30)},
            {BigDecimal.valueOf(500), BigDecimal.valueOf(50)}
    };

    private static final int EXPIRE_DAYS = 30;

    @Autowired
    private CouponMapper couponMapper;

    /** 取待领取的券礼包；手上没有任何可用券时自动补发一整批 */
    public List<UserCoupon> listPending(Integer userId) {
        if (userId == null) return List.of();
        if (couponMapper.countAvailable(userId) == 0) {
            issueBatch(userId);
        }
        return couponMapper.findPendingList(userId);
    }

    /** 一次性发放五档券 */
    public void issueBatch(Integer userId) {
        if (userId == null) return;
        Date expire = Date.from(Instant.now().plus(EXPIRE_DAYS, ChronoUnit.DAYS));
        for (BigDecimal[] tier : TIERS) {
            UserCoupon coupon = new UserCoupon();
            coupon.setUserId(userId);
            coupon.setThresholdAmount(tier[0]);
            coupon.setDiscountAmount(tier[1]);
            coupon.setStatus("pending");
            coupon.setExpireTime(expire);
            couponMapper.insert(coupon);
        }
    }

    /** 一键领取全部待领取券 */
    public int claimAll(Integer userId) {
        if (userId == null) return 0;
        return couponMapper.claimAll(userId);
    }

    /** 我的可用券 */
    public List<UserCoupon> listUnused(Integer userId) {
        return userId == null ? List.of() : couponMapper.findUnused(userId);
    }

    /** 针对某个金额，挑一张最划算的可用券 */
    public UserCoupon findApplicable(Integer userId, BigDecimal amount) {
        if (userId == null || amount == null) return null;
        return couponMapper.findApplicable(userId, amount);
    }

    /** 下单抵扣后核销；五张全部用完后，下次进入首页会自动补发下一批 */
    public void consume(UserCoupon coupon, Integer orderId) {
        if (coupon == null) return;
        couponMapper.markUsed(coupon.getId(), coupon.getUserId(), orderId);
    }
}
