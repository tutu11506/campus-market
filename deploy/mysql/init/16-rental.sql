-- 租赁专区：短期租赁 / 假期寄存转租 / 教材学期租
-- 脚本可重复执行；不预置任何租赁物品或订单，避免伪造经营数据。

CREATE TABLE IF NOT EXISTS `rental_item` (
  `id`                INT            NOT NULL AUTO_INCREMENT COMMENT '出租物品ID',
  `title`             VARCHAR(100)   NOT NULL COMMENT '物品标题',
  `description`       VARCHAR(600)   DEFAULT NULL COMMENT '物品说明、成色、配件',
  `rental_type`       VARCHAR(20)    NOT NULL COMMENT '类型：daily按天/weekly按周/storage假期寄存/textbook教材学期租',
  `category`          VARCHAR(30)    NOT NULL COMMENT '品类：vehicle电动车/digital数码/photo摄影/formal正装/camping露营/dorm宿舍/textbook教材/storage床位柜子/other其他',
  `price`             DECIMAL(10,2)  NOT NULL COMMENT '租金单价',
  `price_unit`        VARCHAR(10)    NOT NULL COMMENT '计价单位：day/week/semester',
  `deposit`           DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '押金（仅登记，平台不代收）',
  `cover_image`       VARCHAR(255)   DEFAULT NULL COMMENT '封面图',
  `location`          VARCHAR(100)   DEFAULT NULL COMMENT '交接/寄存地点',
  `owner_id`          INT            NOT NULL COMMENT '出租人ID',
  `status`            VARCHAR(20)    NOT NULL DEFAULT 'available' COMMENT 'available可租/rented已租出/offline已下架',
  `auto_offline_date` DATE           DEFAULT NULL COMMENT '自动下架日期（假期寄存用于开学下架）',
  `create_time`       DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time`       DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rental_item_status` (`status`,`rental_type`),
  KEY `idx_rental_item_owner` (`owner_id`),
  KEY `idx_rental_item_offline` (`rental_type`,`status`,`auto_offline_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租赁物品表';

CREATE TABLE IF NOT EXISTS `rental_order` (
  `id`                 INT           NOT NULL AUTO_INCREMENT COMMENT '租赁订单ID',
  `order_no`           VARCHAR(32)   NOT NULL COMMENT '订单号',
  `item_id`            INT           NOT NULL COMMENT '出租物品ID',
  `renter_id`          INT           NOT NULL COMMENT '租客ID',
  `owner_id`           INT           NOT NULL COMMENT '出租人ID',
  `rental_type`        VARCHAR(20)   NOT NULL COMMENT '下单时的租赁类型快照',
  `start_date`         DATE          NOT NULL COMMENT '租期开始日期',
  `end_date`           DATE          NOT NULL COMMENT '租期结束日期',
  `rent_amount`        DECIMAL(10,2) NOT NULL COMMENT '租金合计',
  `deposit`            DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '押金快照（线下交接）',
  `status`             VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT 'pending待确认/renting租用中/returning待收回/completed已完成/cancelled已取消',
  `overdue_reminded`   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已发送逾期提醒',
  `due_reminded`       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已发送到期提醒',
  `remark`             VARCHAR(255)  DEFAULT NULL COMMENT '租客备注',
  `handover_note`      VARCHAR(255)  DEFAULT NULL COMMENT '交接说明',
  `confirm_time`       DATETIME      DEFAULT NULL COMMENT '出租方确认时间',
  `return_apply_time`  DATETIME      DEFAULT NULL COMMENT '租客发起归还时间',
  `return_time`        DATETIME      DEFAULT NULL COMMENT '出租方确认收回时间',
  `create_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `update_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_order_no` (`order_no`),
  KEY `idx_rental_order_renter` (`renter_id`,`status`),
  KEY `idx_rental_order_owner` (`owner_id`,`status`),
  KEY `idx_rental_order_item` (`item_id`,`status`),
  KEY `idx_rental_order_due` (`status`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租赁订单表';
