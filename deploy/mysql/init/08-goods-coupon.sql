-- ============================================================
-- 涨价返券：平台在卖家基础价上加价，加价的一半作为优惠券返给买家
--   list_price    平台标价 = base_price + 加价
--   coupon_amount 随单优惠券面额 = 加价 / 2
--   price         券后价（买家实付）= list_price - coupon_amount
-- ============================================================
SET NAMES utf8mb4;

ALTER TABLE goods
  ADD COLUMN base_price DECIMAL(10,2) NULL COMMENT '卖家填写的基础价' AFTER original_price,
  ADD COLUMN list_price DECIMAL(10,2) NULL COMMENT '平台标价（基础价 + 加价）' AFTER base_price,
  ADD COLUMN coupon_amount DECIMAL(10,2) NULL DEFAULT 0 COMMENT '随单优惠券面额（加价的一半）' AFTER list_price;

-- 历史商品不做涨价返券，保持原价
UPDATE goods SET base_price = price, list_price = price, coupon_amount = 0 WHERE list_price IS NULL;
