CREATE TABLE IF NOT EXISTS invoice (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  order_id    INT           NOT NULL,
  order_no    VARCHAR(32)   NOT NULL,
  user_id     INT           NOT NULL COMMENT '申请开票的买家',
  title_type  VARCHAR(16)   NOT NULL DEFAULT 'personal' COMMENT 'personal 个人 / company 企业',
  title       VARCHAR(100)  NOT NULL COMMENT '发票抬头',
  tax_no      VARCHAR(32)   DEFAULT NULL COMMENT '纳税人识别号',
  email       VARCHAR(100)  DEFAULT NULL COMMENT '接收邮箱',
  amount      DECIMAL(10,2) NOT NULL COMMENT '开票金额（实付）',
  invoice_no  VARCHAR(32)   NOT NULL COMMENT '发票号码',
  status      VARCHAR(16)   NOT NULL DEFAULT 'issued' COMMENT 'issued 已开票',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  issue_time  DATETIME      DEFAULT NULL,
  UNIQUE KEY uk_invoice_order (order_id),
  KEY idx_invoice_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子发票';
