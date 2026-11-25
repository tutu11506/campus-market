-- 消息提醒、个性化订阅：心愿单降价 · 教材上新 · 关键词订阅 · 学期定时提醒
-- 说明：本脚本在 MySQL 8 全新初始化时执行；重复执行前请先确认表是否已存在。

/* ==================== 1. 关键词订阅 ==================== */
-- 用户订阅「电路第十版」「电动车」等关键词，平台新上架商品命中即推送站内提醒。
CREATE TABLE IF NOT EXISTS `keyword_subscription` (
  `id`             INT NOT NULL AUTO_INCREMENT,
  `user_id`        INT NOT NULL,
  `keyword`        VARCHAR(40) NOT NULL,
  `notify_new`     TINYINT NOT NULL DEFAULT 1 COMMENT '新上架命中是否提醒 1=是',
  `match_count`    INT NOT NULL DEFAULT 0 COMMENT '累计命中次数',
  `status`         TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `last_notify_at` DATETIME DEFAULT NULL,
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kw_user_keyword` (`user_id`, `keyword`),
  KEY `idx_kw_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='关键词订阅';

/* ==================== 2. 心愿商品（收藏即关注，降价提醒） ==================== */
-- 收藏商品时自动写入一行；商品改价后与 last_price 比较，降价则推送并更新。
CREATE TABLE IF NOT EXISTS `goods_watch` (
  `id`             INT NOT NULL AUTO_INCREMENT,
  `user_id`        INT NOT NULL,
  `goods_id`       INT NOT NULL,
  `last_price`     DECIMAL(10,2) DEFAULT NULL COMMENT '上次记录/提醒时的价格',
  `target_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '期望价（可空）',
  `status`         TINYINT NOT NULL DEFAULT 1 COMMENT '1关注中 0已取消',
  `last_notify_at` DATETIME DEFAULT NULL,
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_watch_user_goods` (`user_id`, `goods_id`),
  KEY `idx_watch_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心愿单商品降价关注';

/* ==================== 3. 教材心愿（上新提醒） ==================== */
-- 用户在教材详情点「想要这本书」后写入；同名教材有新闲置上架即提醒。
CREATE TABLE IF NOT EXISTS `textbook_wish` (
  `id`             INT NOT NULL AUTO_INCREMENT,
  `user_id`        INT NOT NULL,
  `textbook_id`    INT NOT NULL,
  `textbook_title` VARCHAR(120) DEFAULT NULL COMMENT '冗余书名，便于展示',
  `status`         TINYINT NOT NULL DEFAULT 1 COMMENT '1关注中 0已取消',
  `last_notify_at` DATETIME DEFAULT NULL,
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wish_user_textbook` (`user_id`, `textbook_id`),
  KEY `idx_wish_textbook` (`textbook_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教材心愿上新提醒';

/* ==================== 4. 提醒记录（站内推送统一落库） ==================== */
CREATE TABLE IF NOT EXISTS `notify_record` (
  `id`          INT NOT NULL AUTO_INCREMENT,
  `user_id`     INT NOT NULL,
  `notify_type` VARCHAR(24) NOT NULL COMMENT 'price_drop/new_goods/textbook_new/semester_start/graduation',
  `title`       VARCHAR(120) NOT NULL,
  `content`     VARCHAR(500) DEFAULT NULL,
  `link`        VARCHAR(255) DEFAULT NULL COMMENT '点击跳转的路由',
  `goods_id`    INT DEFAULT NULL,
  `textbook_id` INT DEFAULT NULL,
  `keyword`     VARCHAR(40) DEFAULT NULL COMMENT '命中的订阅关键词',
  `ref_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '触发价格，用于同一价格去重',
  `is_read`     TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notify_user` (`user_id`, `is_read`, `create_time`),
  KEY `idx_notify_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内提醒记录';

/* ==================== 5. 学期定时提醒发送记录 ==================== */
-- 同一用户 + 同一提醒类型 + 同一学期只发一次，靠唯一键 INSERT IGNORE 原子去重。
CREATE TABLE IF NOT EXISTS `semester_reminder_log` (
  `id`            INT NOT NULL AUTO_INCREMENT,
  `user_id`       INT NOT NULL,
  `reminder_type` VARCHAR(24) NOT NULL COMMENT 'semester_start/graduation',
  `period_key`    VARCHAR(24) NOT NULL COMMENT '周期标识，如 term-2026 / grad-2026',
  `sent_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_semester_once` (`user_id`, `reminder_type`, `period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学期定时提醒发送记录';
