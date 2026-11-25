-- ============================================================
-- 校友标签：给同学打上「考研党 / 新生 / 毕业清仓 / 摄影爱好者」等标签
-- 用途：按标签筛选，快速找到同需求、同频道的同学与他们的闲置
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_tag` (
  `id`          INT         NOT NULL AUTO_INCREMENT,
  `user_id`     INT         NOT NULL COMMENT '用户ID',
  `tag`         VARCHAR(20) NOT NULL COMMENT '校友标签',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '设置时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`, `tag`),
  KEY `idx_user_tag_tag` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校友标签';

-- 演示数据：按用户 ID 取模分配，保证每个标签都有同学可筛
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '考研党' FROM user WHERE MOD(id, 4) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '新生' FROM user WHERE MOD(id, 7) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '毕业清仓' FROM user WHERE MOD(id, 5) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '摄影爱好者' FROM user WHERE MOD(id, 6) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '健身搭子' FROM user WHERE MOD(id, 9) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '编程开发' FROM user WHERE MOD(id, 8) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '社团达人' FROM user WHERE MOD(id, 11) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '考公党' FROM user WHERE MOD(id, 13) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '乐器玩家' FROM user WHERE MOD(id, 14) = 0;
INSERT IGNORE INTO user_tag(user_id, tag) SELECT id, '留学生' FROM user WHERE MOD(id, 17) = 0;
