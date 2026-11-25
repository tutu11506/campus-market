-- 商品草稿箱 + 一键复制商品
-- 说明：草稿表保存发布页未提交的内容，支持多份草稿；一键复制复用原商品信息生成新的在售商品。

CREATE TABLE IF NOT EXISTS `goods_draft` (
  `id`                 INT NOT NULL AUTO_INCREMENT,
  `user_id`            INT NOT NULL COMMENT '草稿所属用户',
  `title`              VARCHAR(100) DEFAULT NULL,
  `description`        TEXT,
  `price`              DECIMAL(10,2) DEFAULT NULL COMMENT '基础价',
  `original_price`     DECIMAL(10,2) DEFAULT NULL,
  `images`             TEXT COMMENT '图片，逗号分隔的相对路径',
  `cover_image`        VARCHAR(255) DEFAULT NULL,
  `category_id`        INT DEFAULT NULL,
  `textbook_id`        INT DEFAULT NULL,
  `location`           VARCHAR(100) DEFAULT NULL,
  `trade_mode`         VARCHAR(20) NOT NULL DEFAULT 'cash' COMMENT 'cash/exchange',
  `can_mail`           TINYINT NOT NULL DEFAULT 0,
  `can_deliver`        TINYINT NOT NULL DEFAULT 0,
  `exchange_supported` TINYINT NOT NULL DEFAULT 0,
  `exchange_want`      VARCHAR(100) DEFAULT NULL,
  `create_time`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_goods_draft_user` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品草稿箱';
