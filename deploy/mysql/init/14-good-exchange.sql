SET NAMES utf8mb4;

ALTER TABLE goods
  ADD COLUMN can_mail TINYINT NOT NULL DEFAULT 0 COMMENT '可邮寄' AFTER location,
  ADD COLUMN can_deliver TINYINT NOT NULL DEFAULT 0 COMMENT '可送货到宿舍' AFTER can_mail,
  ADD COLUMN trade_mode VARCHAR(16) NOT NULL DEFAULT 'cash' COMMENT 'cash现金/exchange换物' AFTER can_deliver,
  ADD COLUMN exchange_supported TINYINT NOT NULL DEFAULT 0 COMMENT '支持以物换物' AFTER trade_mode,
  ADD COLUMN exchange_want VARCHAR(100) DEFAULT NULL COMMENT '希望换到什么' AFTER exchange_supported;

CREATE TABLE IF NOT EXISTS goods_exchange (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  goods_id         INT          NOT NULL COMMENT '想换的目标商品',
  requester_id     INT          NOT NULL COMMENT '发起换物的人',
  offer_goods_id   INT          NOT NULL COMMENT '对方拿出的自己的商品',
  message          VARCHAR(200) DEFAULT NULL,
  status           VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/accepted/rejected/completed/cancelled',
  requester_done   TINYINT      NOT NULL DEFAULT 0 COMMENT '发起方确认完成',
  owner_done       TINYINT      NOT NULL DEFAULT 0 COMMENT '商品所有者确认完成',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exchange (goods_id, requester_id),
  KEY idx_ex_requester (requester_id, create_time),
  KEY idx_ex_goods (goods_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='以物换物申请';
