-- ============================================================
-- 自动化预警中心：定时扫描库存/临期/滞销/缺货/任务到期
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS automation_alert (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  rule_code   VARCHAR(30)  NOT NULL COMMENT '规则编码',
  level       VARCHAR(10)  NOT NULL DEFAULT 'medium' COMMENT 'high/medium/low',
  title       VARCHAR(120) NOT NULL COMMENT '预警标题',
  content     VARCHAR(500) DEFAULT NULL COMMENT '预警详情',
  target_type VARCHAR(20)  DEFAULT NULL COMMENT 'goods/seller/category/task',
  target_id   INT          DEFAULT NULL COMMENT '关联对象ID',
  user_id     INT          DEFAULT NULL COMMENT '关联用户ID（卖家）',
  alert_date  DATE         NOT NULL COMMENT '预警日期',
  status      VARCHAR(20)  NOT NULL DEFAULT 'open' COMMENT 'open待处理/resolved已处理',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_alert (rule_code, target_type, target_id, alert_date),
  INDEX idx_alert_date (alert_date),
  INDEX idx_alert_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化预警记录';
