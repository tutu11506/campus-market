-- 评审团（类美团「小美评审团」）：买卖双方互评 · 附理由与证据 · 其他用户投票决定是否展示
-- 说明：本脚本在 MySQL 8 全新初始化时执行；重复执行前请先确认表是否已存在。

/* ==================== 1. 评审记录（买卖双方互评） ==================== */

CREATE TABLE IF NOT EXISTS `jury_review` (
  `id`            INT NOT NULL AUTO_INCREMENT,
  `order_id`      INT NOT NULL COMMENT '关联订单',
  `reviewer_id`   INT NOT NULL COMMENT '评价人（买家或卖家）',
  `reviewee_id`   INT NOT NULL COMMENT '被评价人（交易对方）',
  `reviewer_role` VARCHAR(10) NOT NULL COMMENT '评价人身份：buyer/seller',
  `rating`        TINYINT NOT NULL COMMENT '打分 1-5 星',
  `reason`        VARCHAR(500) NOT NULL COMMENT '评价理由',
  `evidence`      VARCHAR(1000) DEFAULT NULL COMMENT '证据图片，逗号分隔的相对地址，最多 3 张',
  `status`        VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending评审中/displayed已展示/hidden不展示',
  `display_votes` INT NOT NULL DEFAULT 0 COMMENT '支持展示票数',
  `hide_votes`    INT NOT NULL DEFAULT 0 COMMENT '反对展示票数',
  `total_votes`   INT NOT NULL DEFAULT 0 COMMENT '总票数',
  `approve_rate`  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '支持率（百分比）',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jury_order_reviewer` (`order_id`, `reviewer_id`),
  KEY `idx_jury_status` (`status`, `create_time`),
  KEY `idx_jury_reviewee` (`reviewee_id`, `status`),
  KEY `idx_jury_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评审团评价';

/* ==================== 2. 评审投票（其他用户投票判断真实性） ==================== */

CREATE TABLE IF NOT EXISTS `jury_vote` (
  `id`          INT NOT NULL AUTO_INCREMENT,
  `review_id`   INT NOT NULL,
  `voter_id`    INT NOT NULL COMMENT '投票用户（不能是评价人或被评价人）',
  `vote`        TINYINT NOT NULL COMMENT '1=支持展示 0=反对展示',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jury_vote` (`review_id`, `voter_id`),
  KEY `idx_jury_vote_review` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评审团投票';
