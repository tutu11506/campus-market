-- ============================================================
-- 平台满减优惠券：领取 → 下单抵扣 → 用完后自动再发一张
--   status: pending 待领取 / unused 已领取可用 / used 已使用
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_coupon (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  user_id          INT NOT NULL,
  threshold_amount DECIMAL(10,2) NOT NULL COMMENT '满多少元可用',
  discount_amount  DECIMAL(10,2) NOT NULL COMMENT '减多少元',
  status           VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending待领取/unused可用/used已使用',
  order_id         INT DEFAULT NULL COMMENT '使用的订单',
  expire_time      DATETIME NOT NULL COMMENT '过期时间',
  used_time        DATETIME DEFAULT NULL,
  create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_uc_user (user_id, status),
  INDEX idx_uc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

ALTER TABLE `order`
  ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '平台优惠券抵扣金额' AFTER price;
