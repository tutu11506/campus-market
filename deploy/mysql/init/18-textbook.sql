-- 教材专区 v2：教材主表 + Goods/Rental 关联 + 课表导入 + 避雷留言 + 笔记附件
-- 脚本可重复执行；Goods 表新增 textbook_id 关联，旧数据默认为 NULL 不影响现有接口。
-- 不预置任何业务数据，避免伪造经营数据。

/* ==================== 1. 教材主表 ==================== */

CREATE TABLE IF NOT EXISTS `textbook` (
  `id`             INT           NOT NULL AUTO_INCREMENT COMMENT '教材主表ID',
  `isbn`           VARCHAR(20)   NOT NULL COMMENT 'ISBN（唯一键：空串表示未录入ISBN）',
  `title`          VARCHAR(120)  NOT NULL COMMENT '书名主标题',
  `subtitle`       VARCHAR(120)  DEFAULT NULL COMMENT '副标题（如"高等数学·第七版"）',
  `author`         VARCHAR(120)  DEFAULT NULL COMMENT '作者（多个用顿号分隔）',
  `publisher`      VARCHAR(80)   DEFAULT NULL COMMENT '出版社',
  `edition`        VARCHAR(40)   DEFAULT NULL COMMENT '版本（如"第七版""2023版"）',
  `subject`        VARCHAR(40)   DEFAULT NULL COMMENT '学科分类（数学/英语/计算机/物理/经济……）',
  `grade`          VARCHAR(40)   DEFAULT NULL COMMENT '适用年级/学期（如"大二下"）',
  `cover_image`    VARCHAR(255)  DEFAULT NULL COMMENT '封面图',
  `teacher_flag`   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否为某老师指定版本 1=是 0=否',
  `teacher_note`   VARCHAR(255)  DEFAULT NULL COMMENT '老师指定版本备注（"李老师只认第七版"）',
  `warn_count`     INT           NOT NULL DEFAULT 0 COMMENT '避雷留言计数',
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '1=有效 0=已隐藏',
  `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_textbook_isbn` (`isbn`),
  KEY `idx_textbook_subject` (`subject`),
  KEY `idx_textbook_teacher` (`teacher_flag`, `warn_count`),
  KEY `idx_textbook_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教材主表（ISBN 聚合 + 老师指定 + 避雷）';

/* Goods/Rental 关联 textbook 主表：旧字段不存在则新增，存在则跳过 */
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'goods' AND COLUMN_NAME = 'textbook_id') = 0,
  'ALTER TABLE goods ADD COLUMN textbook_id INT DEFAULT NULL COMMENT "关联教材主表" AFTER category_id, ADD KEY idx_goods_textbook (textbook_id)',
  'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rental_item' AND COLUMN_NAME = 'textbook_id') = 0,
  'ALTER TABLE rental_item ADD COLUMN textbook_id INT DEFAULT NULL COMMENT "关联教材主表" AFTER category, ADD KEY idx_rental_textbook (textbook_id)',
  'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

/* ==================== 2. 避雷留言 ==================== */

CREATE TABLE IF NOT EXISTS `textbook_warning` (
  `id`           INT          NOT NULL AUTO_INCREMENT,
  `textbook_id`  INT          NOT NULL,
  `user_id`      INT          NOT NULL,
  `reason`       VARCHAR(40)  NOT NULL COMMENT 'why_not_allowed/errors/too_difficult/outdated/other',
  `detail`       VARCHAR(400) DEFAULT NULL COMMENT '详细避雷说明',
  `is_valid`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=有效 0=已被反驳/隐藏',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_warn_textbook` (`textbook_id`, `is_valid`),
  KEY `idx_warn_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教材版本避雷留言';

/* ==================== 3. 课表 ==================== */

CREATE TABLE IF NOT EXISTS `user_course` (
  `id`           INT          NOT NULL AUTO_INCREMENT,
  `user_id`      INT          NOT NULL,
  `course_name`  VARCHAR(80)  NOT NULL COMMENT '课程名（如"高等数学""大学英语"）',
  `teacher`      VARCHAR(40)  DEFAULT NULL COMMENT '授课老师',
  `semester`     VARCHAR(20)  NOT NULL COMMENT '学期（如 2026-2027-1）',
  `course_code`  VARCHAR(20)  DEFAULT NULL COMMENT '可选课代码（导入时保留）',
  `remark`       VARCHAR(120) DEFAULT NULL,
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_user` (`user_id`, `semester`),
  KEY `idx_course_name` (`course_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户课表（用于匹配教材）';

/* ==================== 4. 笔记附件 ==================== */

CREATE TABLE IF NOT EXISTS `textbook_material` (
  `id`           INT          NOT NULL AUTO_INCREMENT,
  `owner_type`   VARCHAR(20)  NOT NULL COMMENT 'textbook 教材主表 / goods 闲置商品 / rental 租赁物品',
  `owner_id`     INT          NOT NULL COMMENT '对应 owner_type 下的主键',
  `uploader_id`  INT          NOT NULL COMMENT '上传人',
  `title`        VARCHAR(100) NOT NULL COMMENT '资料名（如"高数学霸笔记下册"）',
  `description`  VARCHAR(300) DEFAULT NULL,
  `file_type`    VARCHAR(20)  NOT NULL COMMENT 'pdf / ppt / doc / zip / image',
  `file_url`     VARCHAR(500) NOT NULL,
  `file_size`    BIGINT       DEFAULT NULL COMMENT '字节数',
  `delivery_mode` VARCHAR(20)  NOT NULL DEFAULT 'free' COMMENT 'free 免费公开 / bonus 随书附赠 / paid 单独售卖',
  `price`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'delivery_mode=paid 时有效',
  `views`        INT          NOT NULL DEFAULT 0,
  `downloads`    INT          NOT NULL DEFAULT 0,
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=有效 0=已下架',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_material_owner` (`owner_type`, `owner_id`),
  KEY `idx_material_uploader` (`uploader_id`),
  KEY `idx_material_status` (`status`, `delivery_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='笔记/课件/试卷等教材附件';
