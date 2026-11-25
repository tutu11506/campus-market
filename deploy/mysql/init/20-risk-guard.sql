-- 风控 / 防坑 / 自动化：违禁品识别 · 价格监控 · 交易纠纷仲裁 · 平台黑名单
-- 说明：本脚本在 MySQL 8 全新初始化时执行；重复执行前请先确认字段/表是否已存在。

/* ==================== 1. 商品风控字段 ==================== */

ALTER TABLE `goods`
  ADD COLUMN `risk_level` VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT 'none/pass/review/block',
  ADD COLUMN `risk_reason` VARCHAR(300) DEFAULT NULL COMMENT '命中违禁品说明',
  ADD COLUMN `price_alert` TINYINT NOT NULL DEFAULT 0 COMMENT '1=价格高于近期均价',
  ADD COLUMN `price_alert_note` VARCHAR(200) DEFAULT NULL COMMENT '价格预警说明',
  ADD KEY `idx_goods_price_alert` (`price_alert`),
  ADD KEY `idx_goods_risk_level` (`risk_level`);

/* ==================== 2. 违禁品词库（AI 识别规则源） ==================== */

CREATE TABLE IF NOT EXISTS `prohibited_item` (
  `id`          INT NOT NULL AUTO_INCREMENT,
  `category`    VARCHAR(32)  NOT NULL COMMENT 'appliance/e_cigarette/battery/dangerous/other',
  `label`       VARCHAR(40)  NOT NULL COMMENT '展示标签，如「违规电器」',
  `keywords`    VARCHAR(500) NOT NULL COMMENT '逗号分隔关键词（标题/描述/图片名命中即算）',
  `risk_level`  VARCHAR(16)  NOT NULL DEFAULT 'block' COMMENT 'block=直接拦截 review=转人工复核',
  `advice`      VARCHAR(200) DEFAULT NULL COMMENT '拦截提示语',
  `status`      TINYINT      NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_prohibited_category` (`category`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='违禁品识别词库';

INSERT IGNORE INTO `prohibited_item` (`id`, `category`, `label`, `keywords`, `risk_level`, `advice`) VALUES
(1, 'appliance',    '违规大功率电器', '电热锅,热得快,电炉,电磁炉,电饭煲,电水壶,电暖器,小太阳,电热毯,卷发棒,直发板,吹风机,电动车充电器,逆变器', 'block', '宿舍禁用大功率电器，禁止交易'),
(2, 'e_cigarette',  '电子烟及烟油',   '电子烟,烟弹,烟油,雾化器,vape,一次性电子烟,加热不燃烧', 'block', '电子烟及烟油禁止校园内交易'),
(3, 'battery',      '违规电池/电瓶',   '电瓶,铅酸电池,锂电池组,电动车电池,充电宝改装,大容量电池,18650', 'review', '锂电池/电瓶存在安全隐患，需人工复核'),
(4, 'dangerous',    '危险品',         '管制刀具,甩棍,仿真枪,烟花,爆竹,酒精炉,卡式炉,瓦斯罐,易燃', 'block', '危化品及管制器具禁止交易'),
(5, 'medical',      '处方药/医疗器械', '处方药,抗生素,针剂,麻醉,医疗器械,注射器', 'block', '药品及医疗器械禁止个人交易'),
(6, 'fake',         '假冒品牌',        '高仿,复刻,莆田,a货,原单,精仿,一比一', 'review', '疑似假冒品牌，需人工复核'),
(7, 'exam',         '考试作弊器材',    '作弊器,隐形耳机,考试答案,代考,替考', 'block', '考试作弊器材禁止交易');

/* ==================== 3. 图片/内容识别记录 ==================== */

CREATE TABLE IF NOT EXISTS `image_moderation` (
  `id`            INT NOT NULL AUTO_INCREMENT,
  `uploader_id`   INT DEFAULT NULL,
  `goods_id`      INT DEFAULT NULL,
  `image_url`     VARCHAR(500) DEFAULT NULL COMMENT '被检测图片；纯文本检测时为空',
  `text_sample`   VARCHAR(500) DEFAULT NULL COMMENT '检测文本片段',
  `verdict`       VARCHAR(16) NOT NULL COMMENT 'pass/review/block',
  `risk_category` VARCHAR(32) DEFAULT NULL,
  `labels`        VARCHAR(300) DEFAULT NULL COMMENT '命中的标签，逗号分隔',
  `confidence`    DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '0-100 置信度',
  `source`        VARCHAR(16) NOT NULL DEFAULT 'rule' COMMENT 'rule/ai/manual',
  `status`        VARCHAR(16) NOT NULL DEFAULT 'auto' COMMENT 'auto/confirmed/released',
  `handler_id`    INT DEFAULT NULL,
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_moderation_verdict` (`verdict`, `create_time`),
  KEY `idx_moderation_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='违禁品识别记录';

/* ==================== 4. 价格监控预警 ==================== */

CREATE TABLE IF NOT EXISTS `price_alert` (
  `id`          INT NOT NULL AUTO_INCREMENT,
  `goods_id`    INT NOT NULL,
  `seller_id`   INT NOT NULL,
  `title`       VARCHAR(120) DEFAULT NULL,
  `price`       DECIMAL(10,2) NOT NULL,
  `avg_price`   DECIMAL(10,2) NOT NULL COMMENT '近期同类均价',
  `ratio`       DECIMAL(6,2) NOT NULL COMMENT 'price / avg_price',
  `sample_size` INT NOT NULL DEFAULT 0,
  `level`       VARCHAR(16) NOT NULL DEFAULT 'warning' COMMENT 'warning/serious',
  `status`      VARCHAR(16) NOT NULL DEFAULT 'open' COMMENT 'open/handled/dismissed',
  `note`        VARCHAR(300) DEFAULT NULL COMMENT '管理员备注',
  `handler_id`  INT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `handled_at`  DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_price_alert_status` (`status`, `create_time`),
  KEY `idx_price_alert_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='价格异常预警';

/* ==================== 5. 交易纠纷 & 证据 ==================== */

CREATE TABLE IF NOT EXISTS `dispute` (
  `id`             INT NOT NULL AUTO_INCREMENT,
  `order_id`       INT NOT NULL,
  `initiator_id`   INT NOT NULL COMMENT '发起人（买家或卖家）',
  `respondent_id`  INT NOT NULL COMMENT '对方',
  `initiator_role` VARCHAR(10) NOT NULL COMMENT 'buyer/seller',
  `reason_type`    VARCHAR(24) NOT NULL COMMENT 'quality/not_as_described/no_delivery/refund_refused/other',
  `description`    VARCHAR(1000) NOT NULL,
  `claim_amount`   DECIMAL(10,2) DEFAULT NULL COMMENT '诉求金额',
  `status`         VARCHAR(16) NOT NULL DEFAULT 'open' COMMENT 'open/processing/resolved/rejected/withdrawn',
  `verdict`        VARCHAR(24) DEFAULT NULL COMMENT 'buyer_win/seller_win/split/no_fault',
  `verdict_note`   VARCHAR(500) DEFAULT NULL,
  `escrow_frozen`  TINYINT NOT NULL DEFAULT 0 COMMENT '1=已冻结担保资金',
  `admin_id`       INT DEFAULT NULL,
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resolved_at`    DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dispute_status` (`status`, `create_time`),
  KEY `idx_dispute_order` (`order_id`),
  KEY `idx_dispute_initiator` (`initiator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='交易纠纷仲裁';

CREATE TABLE IF NOT EXISTS `dispute_evidence` (
  `id`          INT NOT NULL AUTO_INCREMENT,
  `dispute_id`  INT NOT NULL,
  `uploader_id` INT NOT NULL,
  `type`        VARCHAR(16) NOT NULL COMMENT 'chat/image/text',
  `content`     VARCHAR(1000) DEFAULT NULL COMMENT '聊天记录文字/说明',
  `file_url`    VARCHAR(500) DEFAULT NULL COMMENT '截图/照片地址',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_evidence_dispute` (`dispute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纠纷证据';

/* ==================== 6. 平台黑名单（校内失信共享） ==================== */

CREATE TABLE IF NOT EXISTS `platform_blacklist` (
  `id`           INT NOT NULL AUTO_INCREMENT,
  `user_id`      INT NOT NULL,
  `level`        VARCHAR(16) NOT NULL DEFAULT 'warning' COMMENT 'watch=关注 warning=警告 serious=严重失信',
  `reason`       VARCHAR(300) NOT NULL,
  `evidence`     VARCHAR(500) DEFAULT NULL COMMENT '证据摘要/链接',
  `report_count` INT NOT NULL DEFAULT 0 COMMENT '关联举报数',
  `status`       VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/revoked',
  `created_by`   INT DEFAULT NULL,
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_by`   INT DEFAULT NULL,
  `revoked_at`   DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_platform_blacklist_user` (`user_id`, `status`),
  KEY `idx_platform_blacklist_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台共享黑名单';
