SET NAMES utf8mb4;

ALTER TABLE `order`
  ADD COLUMN escrow_status VARCHAR(16) DEFAULT NULL COMMENT 'held托管中/settled已结算/refunded已退款' AFTER pay_time,
  ADD COLUMN verify_code VARCHAR(8) DEFAULT NULL COMMENT '线下自提核销码' AFTER escrow_status,
  ADD COLUMN verify_time DATETIME DEFAULT NULL COMMENT '核销时间' AFTER verify_code,
  ADD COLUMN settle_time DATETIME DEFAULT NULL COMMENT '资金结算给卖家的时间' AFTER verify_time;

UPDATE `order` SET escrow_status = 'settled', settle_time = update_time
WHERE status = 'completed' AND pay_time IS NOT NULL AND escrow_status IS NULL;

UPDATE `order` SET escrow_status = 'held'
WHERE status IN ('pending', 'dealing') AND pay_time IS NOT NULL AND escrow_status IS NULL;
