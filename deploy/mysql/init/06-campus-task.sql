-- ============================================================
-- 校园任务大厅：同学之间发布 / 接单的悬赏任务
-- 说明：与二手交易解耦，独立一张表，不影响既有业务流程
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS campus_task (
  id           INT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
  title        VARCHAR(100)  NOT NULL COMMENT '任务标题',
  description  VARCHAR(800)  DEFAULT NULL COMMENT '任务描述',
  reward       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '悬赏金额',
  category     VARCHAR(30)   DEFAULT '校园跑腿' COMMENT '任务类型',
  location     VARCHAR(100)  DEFAULT NULL COMMENT '任务地点',
  deadline     DATETIME      DEFAULT NULL COMMENT '期望完成时间',
  publisher_id INT           NOT NULL COMMENT '发布人ID',
  status       VARCHAR(20)   NOT NULL DEFAULT 'open' COMMENT 'open待接单/accepted进行中/completed已完成/cancelled已取消',
  accepter_id  INT           DEFAULT NULL COMMENT '接单人ID',
  accept_time  DATETIME      DEFAULT NULL COMMENT '接单时间',
  complete_time DATETIME     DEFAULT NULL COMMENT '完成时间',
  is_deleted   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_task_status (status),
  INDEX idx_task_publisher (publisher_id),
  INDEX idx_task_accepter (accepter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园任务大厅';

-- 演示数据：用现有用户发布几条任务（表内无用户时不插入）
INSERT INTO campus_task (title, description, reward, category, location, deadline, publisher_id, status)
SELECT '帮取快递到南区宿舍', '两个中等大小的快递，菜鸟驿站取件，送到南区 3 号楼楼下即可。', 6.00, '跑腿代取', '南区宿舍', DATE_ADD(NOW(), INTERVAL 1 DAY), u.id, 'open'
FROM user u ORDER BY u.id LIMIT 1;

INSERT INTO campus_task (title, description, reward, category, location, deadline, publisher_id, status)
SELECT '高数辅导两小时', '大一高数期末复习，帮忙梳理极限和积分部分，图书馆自习室见面。', 40.00, '学习辅导', '图书馆门前', DATE_ADD(NOW(), INTERVAL 3 DAY), u.id, 'open'
FROM user u ORDER BY u.id LIMIT 1;

INSERT INTO campus_task (title, description, reward, category, location, deadline, publisher_id, status)
SELECT '搬宿舍搭把手', '从北区搬到东区，有两个行李箱和一个收纳箱，本校同学一起搬，来回约 40 分钟。', 25.00, '体力帮忙', '北区食堂门口', DATE_ADD(NOW(), INTERVAL 2 DAY), u.id, 'open'
FROM user u ORDER BY u.id LIMIT 1;
