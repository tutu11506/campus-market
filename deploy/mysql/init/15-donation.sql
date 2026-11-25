SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS donation_org (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(50)  NOT NULL COMMENT '公益社团名称',
  intro           VARCHAR(255) NOT NULL COMMENT '社团及捐赠说明',
  accept_types    VARCHAR(32)  NOT NULL COMMENT '可接收品类：clothes,books',
  contact         VARCHAR(100) NOT NULL COMMENT '对接联系方式',
  dropoff_address VARCHAR(100) NOT NULL COMMENT '自行交接接收点',
  is_demo         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否演示资料',
  is_active       TINYINT      NOT NULL DEFAULT 1,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_donation_org_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公益捐赠对接社团';

CREATE TABLE IF NOT EXISTS donation_record (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  user_id           INT          NOT NULL COMMENT '捐赠人',
  org_id            INT          NOT NULL COMMENT '接收社团',
  item_type         VARCHAR(16)  NOT NULL COMMENT 'clothes衣物/books书籍',
  item_name         VARCHAR(100) NOT NULL COMMENT '物品名称',
  quantity          INT          NOT NULL COMMENT '登记数量',
  condition_level   VARCHAR(16)  NOT NULL COMMENT 'new全新/good良好/fair一般',
  handover_method   VARCHAR(16)  NOT NULL COMMENT 'dropoff自送/pickup预约取件',
  handover_detail   VARCHAR(255) NOT NULL COMMENT '交接时间、地点或宿舍说明',
  contact           VARCHAR(50)  NOT NULL COMMENT '捐赠人联系方式',
  remark            VARCHAR(500) DEFAULT NULL,
  status            VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending待交接/received已接收/cancelled已取消',
  received_quantity INT          NOT NULL DEFAULT 0 COMMENT '实际接收数量',
  received_by       INT          DEFAULT NULL COMMENT '确认实收的管理员',
  received_time     DATETIME     DEFAULT NULL,
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_donation_user (user_id, create_time),
  KEY idx_donation_status (status, create_time),
  KEY idx_donation_org (org_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公益捐赠登记与实收记录';

INSERT IGNORE INTO donation_org
  (name, intro, accept_types, contact, dropoff_address, is_demo)
VALUES
  ('绿芽衣物公益社', '接收清洁、无破损的四季衣物，整理后用于校园公益帮扶。', 'clothes', '演示对接员：000-00000000', '大学生活动中心 A101（演示接收点）', 1),
  ('书香接力公益社', '接收内容完整的教材、教辅与课外书籍，让闲置图书继续流转。', 'books', '演示对接员：000-00000001', '图书馆一层服务台（演示接收点）', 1);
