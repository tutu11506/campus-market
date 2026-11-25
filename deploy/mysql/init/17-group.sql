-- 拼单 & 搭子：拼单（全新团购/分装/共享会员/二手分摊）与搭子组队（装备拼买/跳蚤市场/考研学习/运动/其他）
-- 脚本可重复执行；不预置任何业务数据，避免伪造经营数据。

CREATE TABLE IF NOT EXISTS `group_buy` (
  `id`               INT           NOT NULL AUTO_INCREMENT COMMENT '拼单ID',
  `owner_id`         INT           NOT NULL COMMENT '发起人ID（自动占 1 份）',
  `type`             VARCHAR(20)   NOT NULL COMMENT '类型：fresh全新团购/split拼分装/membership共享会员/secondhand二手大件分摊',
  `title`            VARCHAR(100)  NOT NULL COMMENT '拼单标题',
  `description`      VARCHAR(600)  DEFAULT NULL COMMENT '拼单说明：规格、分摊方式、结算约定',
  `target_count`     INT           NOT NULL COMMENT '目标份数（含发起人 1 份）',
  `joined_count`     INT           NOT NULL DEFAULT 1 COMMENT '已拼份数（含发起人 1 份）',
  `price_per_share`  DECIMAL(10,2) NOT NULL COMMENT '每份费用（仅供展示，线下结算）',
  `deadline`         DATETIME      NOT NULL COMMENT '拼单截止时间',
  `goods_id`         INT           DEFAULT NULL COMMENT '关联平台商品ID（可选）',
  `location`         VARCHAR(100)  DEFAULT NULL COMMENT '取货/分摊地点',
  `status`           VARCHAR(20)   NOT NULL DEFAULT 'open' COMMENT 'open拼单中/full已满员/formed已成团/failed未成团/closed已关闭',
  `deadline_reminded` TINYINT      NOT NULL DEFAULT 0 COMMENT '截止失败提醒是否已发送',
  `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_buy_status` (`status`,`deadline`),
  KEY `idx_group_buy_owner` (`owner_id`,`status`),
  KEY `idx_group_buy_type` (`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拼单表';

CREATE TABLE IF NOT EXISTS `group_buy_member` (
  `id`          INT       NOT NULL AUTO_INCREMENT COMMENT '拼单成员ID',
  `group_id`    INT       NOT NULL COMMENT '拼单ID',
  `user_id`     INT       NOT NULL COMMENT '参与用户ID',
  `shares`      INT       NOT NULL DEFAULT 1 COMMENT '参与份数',
  `status`      VARCHAR(20) NOT NULL DEFAULT 'joined' COMMENT 'joined已参与/left已退出',
  `create_time` DATETIME  DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
  `update_time` DATETIME  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_buy_member` (`group_id`,`user_id`),
  KEY `idx_group_buy_member_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拼单成员表';

CREATE TABLE IF NOT EXISTS `partner_post` (
  `id`            INT           NOT NULL AUTO_INCREMENT COMMENT '搭子ID',
  `owner_id`      INT           NOT NULL COMMENT '发起人ID（自动占 1 人）',
  `type`          VARCHAR(20)   NOT NULL COMMENT '类型：gear装备拼买/flea跳蚤市场/exam考研学习/sport运动/other其他',
  `title`         VARCHAR(100)  NOT NULL COMMENT '搭子标题',
  `description`   VARCHAR(600)  DEFAULT NULL COMMENT '说明：集合方式、分工、注意事项',
  `activity_time` DATETIME      NOT NULL COMMENT '活动时间',
  `location`      VARCHAR(100)  DEFAULT NULL COMMENT '活动/集合地点',
  `member_limit`  INT           NOT NULL COMMENT '人数上限（含发起人 1 人）',
  `member_count`  INT           NOT NULL DEFAULT 1 COMMENT '当前人数（含发起人 1 人）',
  `link_type`     VARCHAR(20)   NOT NULL DEFAULT 'none' COMMENT '关联类型：none/goods闲置商品/groupbuy拼单',
  `link_id`       INT           DEFAULT NULL COMMENT '关联对象ID',
  `link_title`    VARCHAR(120)  DEFAULT NULL COMMENT '关联对象标题快照',
  `status`        VARCHAR(20)   NOT NULL DEFAULT 'open' COMMENT 'open招募中/full已满员/finished已结束/cancelled已取消',
  `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_partner_status` (`status`,`activity_time`),
  KEY `idx_partner_owner` (`owner_id`,`status`),
  KEY `idx_partner_type` (`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搭子组队表';

CREATE TABLE IF NOT EXISTS `partner_member` (
  `id`          INT         NOT NULL AUTO_INCREMENT COMMENT '搭子成员ID',
  `partner_id`  INT         NOT NULL COMMENT '搭子ID',
  `user_id`     INT         NOT NULL COMMENT '参与用户ID',
  `status`      VARCHAR(20) NOT NULL DEFAULT 'joined' COMMENT 'joined已加入/left已退出',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_partner_member` (`partner_id`,`user_id`),
  KEY `idx_partner_member_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搭子成员表';
