-- ============================================================
-- 校园论坛：帖子 / 回复 / 点赞
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS forum_post (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  board         VARCHAR(20)  NOT NULL DEFAULT 'chat' COMMENT 'chat闲聊/lost失物/lost_found/study学习/team组队',
  title         VARCHAR(120) NOT NULL,
  content       TEXT         NOT NULL,
  user_id       INT          NOT NULL,
  view_count    INT          NOT NULL DEFAULT 0,
  like_count    INT          NOT NULL DEFAULT 0,
  comment_count INT          NOT NULL DEFAULT 0,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_post_board (board, create_time),
  INDEX idx_post_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子';

CREATE TABLE IF NOT EXISTS forum_comment (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  post_id    INT      NOT NULL,
  user_id    INT      NOT NULL,
  parent_id  INT      NULL COMMENT '回复的评论ID，NULL表示直接回复帖子',
  reply_to_user_id INT NULL COMMENT '被回复的用户ID',
  content    VARCHAR(1000) NOT NULL,
  is_deleted TINYINT  NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_comment_post (post_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛回复';

CREATE TABLE IF NOT EXISTS forum_notification (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  user_id      INT          NOT NULL COMMENT '接收通知的用户',
  type         VARCHAR(20)  NOT NULL COMMENT 'post_reply 帖子被回复 / comment_reply 回复被回复',
  post_id      INT          NOT NULL,
  comment_id   INT          NOT NULL,
  from_user_id INT          NOT NULL COMMENT '触发通知的用户',
  post_title   VARCHAR(120) NOT NULL,
  content      VARCHAR(200) NOT NULL COMMENT '回复内容摘要',
  is_read      TINYINT      NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notice_user (user_id, is_read, create_time),
  INDEX idx_notice_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛通知';

CREATE TABLE IF NOT EXISTS forum_like (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  post_id     INT      NOT NULL,
  user_id     INT      NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_like (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞';
