-- ============================================================
-- 闲置盲盒许愿：买家按品类许愿（文具 / 书籍等），卖家打包随机发货
-- 状态流转：open 待认领 -> matched 已打包待确认 -> completed 已完成
--          open 也可被买家取消 -> cancelled 已取消
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `blind_box_wish` (
  `id`            INT           NOT NULL AUTO_INCREMENT,
  `buyer_id`      INT           NOT NULL COMMENT '许愿买家',
  `category`      VARCHAR(20)   NOT NULL COMMENT '许愿品类：文具/书籍/数码配件/生活杂物/随机',
  `budget`        DECIMAL(10,2) NOT NULL DEFAULT 20.00 COMMENT '买家预算上限',
  `wish_note`     VARCHAR(200)  DEFAULT NULL COMMENT '许愿说明',
  `status`        VARCHAR(20)   NOT NULL DEFAULT 'open' COMMENT 'open/matched/completed/cancelled',
  `seller_id`     INT           DEFAULT NULL COMMENT '认领卖家',
  `box_title`     VARCHAR(60)   DEFAULT NULL COMMENT '盲盒标题',
  `box_desc`      VARCHAR(300)  DEFAULT NULL COMMENT '盲盒描述',
  `price`         DECIMAL(10,2) DEFAULT NULL COMMENT '成交价',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '许愿时间',
  `match_time`    DATETIME      DEFAULT NULL COMMENT '认领打包时间',
  `complete_time` DATETIME      DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_blind_box_status` (`status`),
  KEY `idx_blind_box_buyer` (`buyer_id`),
  KEY `idx_blind_box_seller` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲置盲盒许愿';

-- 演示数据：取几位普通同学各挂一条待认领许愿（已有同类许愿则不重复插入）
INSERT INTO blind_box_wish(buyer_id, category, budget, wish_note)
SELECT id, '文具', 15.00, '期末复习想要几支好写的黑笔和一本空白笔记本，惊喜就好～'
FROM user
WHERE username = '电脑达人小王'
AND NOT EXISTS (SELECT 1 FROM blind_box_wish WHERE category = '文具' AND status = 'open');

INSERT INTO blind_box_wish(buyer_id, category, budget, wish_note)
SELECT id, '书籍', 30.00, '想收两三本专业课相关的课外书，专业不限，缘分开盲盒'
FROM user
WHERE username = '毕业清仓小李'
AND NOT EXISTS (SELECT 1 FROM blind_box_wish WHERE category = '书籍' AND status = 'open');

INSERT INTO blind_box_wish(buyer_id, category, budget, wish_note)
SELECT id, '生活杂物', 20.00, '宿舍刚搬完，缺一些小收纳、挂钩之类的实用小物'
FROM user
WHERE username = '测试同学'
AND NOT EXISTS (SELECT 1 FROM blind_box_wish WHERE category = '生活杂物' AND status = 'open');
