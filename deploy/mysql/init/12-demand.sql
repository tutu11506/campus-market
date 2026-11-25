CREATE TABLE IF NOT EXISTS demand_post (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  user_id      INT           NOT NULL COMMENT '发布求购的学生',
  title        VARCHAR(100)  NOT NULL COMMENT '求购标题',
  category     VARCHAR(20)   NOT NULL DEFAULT 'other' COMMENT 'textbook/vehicle/furniture/digital/dorm/other',
  keyword      VARCHAR(50)   DEFAULT NULL COMMENT '匹配在售商品的关键词',
  description  VARCHAR(1000) DEFAULT NULL,
  budget_min   DECIMAL(10,2) DEFAULT NULL,
  budget_max   DECIMAL(10,2) DEFAULT NULL,
  target_major VARCHAR(50)   DEFAULT NULL COMMENT '定向专业',
  target_grade VARCHAR(20)   DEFAULT NULL COMMENT '定向年级',
  status       VARCHAR(16)   NOT NULL DEFAULT 'open' COMMENT 'open 求购中 / matched 已成交 / closed 已关闭',
  quote_count  INT           NOT NULL DEFAULT 0,
  deal_price   DECIMAL(10,2) DEFAULT NULL COMMENT '成交价',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT       NOT NULL DEFAULT 0,
  INDEX idx_demand_status (status, create_time),
  INDEX idx_demand_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求购发布';

CREATE TABLE IF NOT EXISTS demand_quote (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  demand_id   INT           NOT NULL,
  seller_id   INT           NOT NULL,
  price       DECIMAL(10,2) NOT NULL,
  message     VARCHAR(500)  DEFAULT NULL,
  status      VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending 待选择 / accepted 已接单 / rejected 未选中',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_quote (demand_id, seller_id),
  KEY idx_quote_demand (demand_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求购报价';
