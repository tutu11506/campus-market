-- 积分 & 环保激励系统：循环碳积分 / 积分商城 / 环保勋章 / 环保排行榜
-- 脚本可重复执行（IF NOT EXISTS）

/* ==================== 1. 积分账户 ==================== */

CREATE TABLE IF NOT EXISTS `point_account` (
  `user_id`         INT          NOT NULL,
  `balance`         INT          NOT NULL DEFAULT 0 COMMENT '当前可用积分',
  `earned_total`    INT          NOT NULL DEFAULT 0 COMMENT '累计获得',
  `spent_total`     INT          NOT NULL DEFAULT 0 COMMENT '累计消费',
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_point_account_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户积分账户';

/* ==================== 2. 积分流水 ==================== */

CREATE TABLE IF NOT EXISTS `point_record` (
  `id`              INT          NOT NULL AUTO_INCREMENT,
  `user_id`         INT          NOT NULL,
  `direction`       TINYINT      NOT NULL COMMENT '1=获得 -1=消费',
  `points`          INT          NOT NULL COMMENT '本次变动积分（正数）',
  `balance_after`   INT          NOT NULL COMMENT '变动后余额',
  `type`            VARCHAR(32)  NOT NULL COMMENT '发布闲置/成交/发布可租/完成租赁/捐赠/兑换/管理员调整',
  `ref_type`        VARCHAR(24)  DEFAULT NULL COMMENT '关联对象类型:goods/rental/donation/reward/manual',
  `ref_id`          INT          DEFAULT NULL COMMENT '关联对象 ID',
  `remark`          VARCHAR(200) DEFAULT NULL COMMENT '备注/商品标题等',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_point_record_user` (`user_id`, `created_at` DESC),
  KEY `idx_point_record_ref` (`ref_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分流水';

/* ==================== 3. 积分兑换商品 ==================== */

CREATE TABLE IF NOT EXISTS `point_reward` (
  `id`              INT          NOT NULL AUTO_INCREMENT,
  `name`            VARCHAR(80)  NOT NULL COMMENT '兑换名：跑腿优惠券/商品置顶/集市摊位/环保勋章',
  `icon`            VARCHAR(50)  DEFAULT NULL COMMENT 'emoji 图标',
  `type`            VARCHAR(24)  NOT NULL COMMENT 'coupon/top/stall/badge/cashback',
  `price_points`    INT          NOT NULL COMMENT '所需积分',
  `stock`           INT          DEFAULT NULL COMMENT '库存（NULL 无限）',
  `total_redeemed`  INT          NOT NULL DEFAULT 0 COMMENT '已兑换次数',
  `description`     VARCHAR(300) DEFAULT NULL,
  `config_json`     VARCHAR(500) DEFAULT NULL COMMENT '配置：如置顶天数、优惠券面额',
  `sort_order`      INT          NOT NULL DEFAULT 0,
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_point_reward_type` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分兑换商品';

/* ==================== 4. 兑换订单 ==================== */

CREATE TABLE IF NOT EXISTS `point_exchange` (
  `id`              INT          NOT NULL AUTO_INCREMENT,
  `user_id`         INT          NOT NULL,
  `reward_id`       INT          NOT NULL,
  `reward_snapshot` VARCHAR(500) DEFAULT NULL COMMENT '兑换时的 reward 名称+类型快照',
  `points_cost`     INT          NOT NULL,
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=已发放 0=处理中 -1=失败已退积分',
  `delivered_at`    DATETIME     DEFAULT NULL,
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_point_exchange_user` (`user_id`, `created_at` DESC),
  KEY `idx_point_exchange_reward` (`reward_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分兑换订单';

/* ==================== 5. 环保勋章 ==================== */

CREATE TABLE IF NOT EXISTS `eco_badge` (
  `id`              INT          NOT NULL AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL COMMENT 'bronze_cycler / silver_cycler / gold_cycler / donor_master 等',
  `name`            VARCHAR(40)  NOT NULL,
  `icon`            VARCHAR(100) DEFAULT NULL COMMENT 'emoji 或图标字符',
  `description`     VARCHAR(200) DEFAULT NULL,
  `condition_type`  VARCHAR(24)  NOT NULL COMMENT 'total_points / goods_sold / donation_count / rental_complete',
  `condition_value` INT          NOT NULL COMMENT '触发阈值',
  `reward_points`   INT          NOT NULL DEFAULT 0 COMMENT '达成后额外奖励积分',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eco_badge_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='环保勋章定义';

/* ==================== 6. 用户勋章 ==================== */

CREATE TABLE IF NOT EXISTS `user_eco_badge` (
  `id`              INT          NOT NULL AUTO_INCREMENT,
  `user_id`         INT          NOT NULL,
  `badge_id`        INT          NOT NULL,
  `unlocked_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_badge` (`user_id`, `badge_id`),
  KEY `idx_ueb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户已解锁环保勋章';

/* ==================== 7. 预置勋章 & 兑换商品（INSERT IGNORE 可重复执行） ==================== */

INSERT IGNORE INTO `eco_badge` (`code`, `name`, `icon`, `description`, `condition_type`, `condition_value`, `reward_points`) VALUES
('bronze_cycler',  '青铜循环者', '🥉', '成功流转 3 件闲置',   'goods_sold',       3,   50),
('silver_cycler',  '白银循环者', '🥈', '成功流转 10 件闲置',  'goods_sold',       10,  150),
('gold_cycler',    '黄金循环者', '🥇', '成功流转 30 件闲置',  'goods_sold',       30,  400),
('donor_master',   '捐赠达人',   '💚', '累计捐赠 5 次',       'donation_count',   5,   120),
('rental_hero',    '租赁英雄',   '📖', '累计完成 8 次租赁',   'rental_complete',  8,   100),
('eco_explorer',   '环保探索者', '🌱', '累计获得 200 积分',   'total_points',     200, 0),
('eco_pioneer',    '环保先锋',   '🌍', '累计获得 800 积分',   'total_points',     800, 200);

INSERT IGNORE INTO `point_reward` (`name`, `icon`, `type`, `price_points`, `stock`, `total_redeemed`, `description`, `config_json`, `sort_order`, `status`) VALUES
('跑腿立减 5 元',       '🛵', 'coupon',   100,  500, 0, '校园闪送跑腿订单可用，限时 7 天',  '{"couponValue":5,"validDays":7}',        1, 1),
('商品置顶 1 天',       '📌', 'top',      200,  200, 0, '一件闲置商品在首页置顶 24 小时',   '{"days":1}',                             2, 1),
('跑腿立减 10 元',      '🛵', 'coupon',   180,  300, 0, '校园闪送跑腿订单可用，限时 14 天', '{"couponValue":10,"validDays":14}',      3, 1),
('商品置顶 3 天',       '📌', 'top',      500,  100, 0, '一件闲置商品在首页置顶 72 小时',   '{"days":3}',                             4, 1),
('线下集市摊位 1 个',   '🎪', 'stall',    1200, 30,  0, '校园跳蚤集市摊位名额 1 个（需提前预约）', '{"desc":"线下集市摊位"}',             5, 1),
('环保达人勋章（虚拟）','🏅', 'badge',    1000, NULL,0, '解锁平台「环保达人」虚拟勋章，展示于个人主页', '{"badgeCode":"eco_explorer"}',       6, 1),
('现金抵扣 2 元',       '💰', 'cashback', 300,  NULL,0, '捐赠或跑腿运费抵扣，不找零',        '{"cashValue":2}',                        7, 1);
