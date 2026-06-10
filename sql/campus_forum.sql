-- =============================================
-- Campus Forum 数据库初始化脚本
-- 数据库：campus_forum
-- 字符集：utf8mb4
-- =============================================

CREATE DATABASE IF NOT EXISTS `campus_forum`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `campus_forum`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户账户表
-- ----------------------------
DROP TABLE IF EXISTS `db_account`;
CREATE TABLE `db_account` (
  `id`            int NOT NULL AUTO_INCREMENT   COMMENT '用户ID，主键自增',
  `username`      varchar(255) DEFAULT NULL     COMMENT '用户名',
  `password`      varchar(255) DEFAULT NULL     COMMENT '密码（BCrypt加密存储）',
  `email`         varchar(255) DEFAULT NULL     COMMENT '邮箱地址，用于注册和找回密码',
  `role`          varchar(255) DEFAULT 'user'   COMMENT '角色：user=普通用户, admin=管理员, super_admin=超级管理员',
  `status`        varchar(255) DEFAULT 'active' COMMENT '账号状态：active=正常, disabled=禁用',
  `avatar`        varchar(255) DEFAULT NULL     COMMENT '头像路径（MinIO存储地址）',
  `register_time` datetime DEFAULT NULL         COMMENT '注册时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户表，存储用户的基本登录信息';

BEGIN;
INSERT INTO `db_account` (`id`, `username`, `password`, `email`, `role`, `status`, `avatar`, `register_time`) VALUES
(1, 'super_admin', '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'super_admin@campus.com', 'super_admin', 'active', NULL, '2023-08-27 00:00:00'),
(2, 'test', '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'test@campus.com', 'user', 'active', NULL, '2023-08-27 00:18:20');
COMMIT;

-- ----------------------------
-- 用户详情表
-- ----------------------------
DROP TABLE IF EXISTS `db_account_details`;
CREATE TABLE `db_account_details` (
  `id`     int NOT NULL              COMMENT '用户ID，与 db_account.id 一致',
  `gender` tinyint DEFAULT NULL      COMMENT '性别：0=未设置, 1=男, 2=女',
  `phone`  varchar(255) DEFAULT NULL COMMENT '手机号码',
  `qq`     varchar(255) DEFAULT NULL COMMENT 'QQ号',
  `wx`     varchar(255) DEFAULT NULL COMMENT '微信号',
  `desc`   varchar(255) DEFAULT NULL COMMENT '个人简介',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户详情表，存储用户的个人资料信息，与 db_account 一对一关系';

BEGIN;
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES
(1, 0, NULL, NULL, NULL, '超级管理员'),
(2, 0, NULL, NULL, NULL, '测试用户');
COMMIT;

-- ----------------------------
-- 用户隐私设置表
-- ----------------------------
DROP TABLE IF EXISTS `db_account_privacy`;
CREATE TABLE `db_account_privacy` (
  `id`     int NOT NULL      COMMENT '用户ID，与 db_account.id 一致',
  `phone`  tinyint DEFAULT 0 COMMENT '手机号是否公开：0=隐藏, 1=公开',
  `email`  tinyint DEFAULT 0 COMMENT '邮箱是否公开：0=隐藏, 1=公开',
  `wx`     tinyint DEFAULT 0 COMMENT '微信是否公开：0=隐藏, 1=公开',
  `qq`     tinyint DEFAULT 0 COMMENT 'QQ是否公开：0=隐藏, 1=公开',
  `gender` tinyint DEFAULT 0 COMMENT '性别是否公开：0=隐藏, 1=公开',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户隐私设置表，控制用户个人资料中哪些信息对外公开';

BEGIN;
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES
(1, 0, 0, 0, 0, 0),
(2, 0, 1, 0, 1, 1);
COMMIT;

-- ----------------------------
-- 帖子分类表
-- system_key:
--   NULL     = 普通分类
--   activity = 校园活动（系统分类）
--   notice   = 教务通知（系统分类）
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_type`;
CREATE TABLE `db_topic_type` (
  `id`         int NOT NULL AUTO_INCREMENT COMMENT '分类ID，主键自增',
  `name`       varchar(255) DEFAULT NULL   COMMENT '分类名称',
  `desc`       varchar(255) DEFAULT NULL   COMMENT '分类描述',
  `color`      varchar(255) DEFAULT NULL   COMMENT '标签颜色（十六进制色值）',
  `system_key` varchar(32) DEFAULT NULL    COMMENT '系统分类标识：activity/notice',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_topic_type_system_key` (`system_key`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子分类表，定义论坛的帖子分类，由管理员在后台维护';

BEGIN;
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`, `system_key`) VALUES
(1, '日常闲聊', '在这里分享你的各种日常', '#1E90FF', NULL),
(2, '真诚交友', '在校园里寻找与自己志同道合的朋友', '#CE1EFF', NULL),
(3, '问题反馈', '反馈你在校园里遇到的问题', '#E07373', NULL),
(4, '恋爱官宣', '向大家展示你的恋爱成果', '#E0CE73', NULL),
(5, '踩坑记录', '将你遇到的坑分享给大家，防止其他人再次入坑', '#3BB62A', NULL),
(6, '校园活动', '管理员发布的校园活动信息', '#409EFF', 'activity'),
(7, '教务通知', '管理员发布的教务通知', '#E6A23C', 'notice');
COMMIT;

-- ----------------------------
-- 帖子表
-- 帖子生命周期：pending_review → published / rejected / hidden / deleted
-- allow_comment:
--   1 = 允许评论
--   0 = 不允许评论（教务通知默认关闭）
-- ----------------------------
DROP TABLE IF EXISTS `db_topic`;
CREATE TABLE `db_topic` (
  `id`               int NOT NULL AUTO_INCREMENT           COMMENT '帖子ID，主键自增',
  `title`            varchar(255) DEFAULT NULL             COMMENT '帖子标题',
  `content`          text DEFAULT NULL                     COMMENT '帖子内容（JSON格式，Quill富文本编辑器的Delta数据）',
  `uid`              int DEFAULT NULL                      COMMENT '发帖用户ID，关联 db_account.id',
  `type`             int DEFAULT NULL                      COMMENT '分类ID，关联 db_topic_type.id',
  `time`             datetime DEFAULT NULL                 COMMENT '创建时间',
  `top`              tinyint DEFAULT 0                     COMMENT '是否置顶：0=否, 1=是',
  `status`           varchar(255) DEFAULT 'pending_review' COMMENT '帖子状态：pending_review=待审核, published=已发布, rejected=已拒绝, hidden=已隐藏, deleted=已删除',
  `allow_comment`    tinyint(1) NOT NULL DEFAULT 1         COMMENT '是否允许评论：1=允许,0=不允许',
  `view_count`       int NOT NULL DEFAULT 0                COMMENT '浏览量',
  `review_time`      datetime DEFAULT NULL                 COMMENT '最近一次审核时间',
  `review_by`        int DEFAULT NULL                      COMMENT '审核人ID，关联 db_account.id',
  `review_reason`    varchar(255) DEFAULT NULL             COMMENT '审核理由（如拒绝原因）',
  `hide_reason`      varchar(255) DEFAULT NULL             COMMENT '下架理由（管理员下架时填写）',
  `last_submit_time` datetime DEFAULT NULL                 COMMENT '最后提交审核时间（编辑后重新提交会更新）',
  `deleted_time`     datetime DEFAULT NULL                 COMMENT '删除时间（软删除）',
  `deleted_by`       int DEFAULT NULL                      COMMENT '删除人ID，关联 db_account.id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子表，核心业务表，存储论坛帖子内容和审核状态';

-- ----------------------------
-- 校园活动扩展表
-- 仅当帖子分类为 activity 时使用
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_activity`;
CREATE TABLE `db_topic_activity` (
  `tid`             int NOT NULL          COMMENT '关联帖子ID',
  `activity_time`   datetime NOT NULL     COMMENT '活动时间',
  `location`        varchar(100) NOT NULL COMMENT '活动地点',
  `organizer`       varchar(100) NOT NULL COMMENT '主办方',
  `signup_deadline` datetime DEFAULT NULL COMMENT '报名截止时间',
  PRIMARY KEY (`tid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园活动扩展表，存储活动时间、地点、主办方、报名截止时间';

-- ----------------------------
-- 评论表
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_comment`;
CREATE TABLE `db_topic_comment` (
  `id`      int NOT NULL AUTO_INCREMENT          COMMENT '评论ID，主键自增',
  `uid`     int DEFAULT NULL                     COMMENT '评论用户ID，关联 db_account.id',
  `tid`     int DEFAULT NULL                     COMMENT '所属帖子ID，关联 db_topic.id',
  `content` text DEFAULT NULL                    COMMENT '评论内容（JSON格式，Quill富文本Delta数据）',
  `time`    datetime DEFAULT NULL                COMMENT '评论时间',
  `quote`   int DEFAULT NULL                     COMMENT '引用的评论ID：-1=顶级评论，其他值=回复的评论ID',
  `status`  varchar(255) DEFAULT 'normal'        COMMENT '评论状态：normal=正常, deleted=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表，存储帖子下的评论，支持嵌套回复';

-- ----------------------------
-- 点赞记录表
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_interact_like`;
CREATE TABLE `db_topic_interact_like` (
  `tid`  int DEFAULT NULL          COMMENT '帖子ID，关联 db_topic.id',
  `uid`  int DEFAULT NULL          COMMENT '点赞用户ID，关联 db_account.id',
  `time` datetime DEFAULT NULL     COMMENT '点赞时间',
  UNIQUE KEY `tid_uid_like` (`tid`, `uid`)  COMMENT '联合唯一键：同一用户对同一帖子只能点赞一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点赞记录表，记录用户对帖子的点赞行为';

-- ----------------------------
-- 收藏记录表
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_interact_collect`;
CREATE TABLE `db_topic_interact_collect` (
  `tid`  int DEFAULT NULL          COMMENT '帖子ID，关联 db_topic.id',
  `uid`  int DEFAULT NULL          COMMENT '收藏用户ID，关联 db_account.id',
  `time` datetime DEFAULT NULL     COMMENT '收藏时间',
  UNIQUE KEY `tid_uid_collect` (`tid`, `uid`)  COMMENT '联合唯一键：同一用户对同一帖子只能收藏一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏记录表，记录用户对帖子的收藏行为';

-- ----------------------------
-- 通知表
-- ----------------------------
DROP TABLE IF EXISTS `db_notification`;
CREATE TABLE `db_notification` (
  `id`      int NOT NULL AUTO_INCREMENT  COMMENT '通知ID，主键自增',
  `uid`     int DEFAULT NULL             COMMENT '接收通知的用户ID，关联 db_account.id',
  `title`   varchar(255) DEFAULT NULL    COMMENT '通知标题',
  `content` varchar(255) DEFAULT NULL    COMMENT '通知内容',
  `type`    varchar(255) DEFAULT NULL    COMMENT '通知类型',
  `url`     varchar(255) DEFAULT NULL    COMMENT '跳转链接（点击通知后跳转到哪个页面）',
  `time`    datetime DEFAULT NULL        COMMENT '通知时间',
  `status`  varchar(20) DEFAULT 'unread' COMMENT '阅读状态：unread=未读，read=已读',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表，存储系统通知（如帖子审核结果、评论回复提醒等）';

-- ----------------------------
-- 论坛公告表
-- 单条纯文本公告
-- ----------------------------
DROP TABLE IF EXISTS `db_forum_notice`;
CREATE TABLE `db_forum_notice` (
  `id`          int NOT NULL                COMMENT '公告ID，单条公告固定为1',
  `content`     text NOT NULL               COMMENT '公告正文（纯文本）',
  `update_time` datetime DEFAULT NULL       COMMENT '更新时间',
  `update_by`   int DEFAULT NULL            COMMENT '更新人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='论坛公告表，仅维护单条公告';

BEGIN;
INSERT INTO `db_forum_notice` (`id`, `content`, `update_time`, `update_by`) VALUES
(1, '欢迎来到 Campus Forum。请文明发言，友善交流，共同维护良好的校园社区氛围。', NOW(), 1);

-- =============================================
-- 举报记录表
-- =============================================
DROP TABLE IF EXISTS `db_report`;
CREATE TABLE `db_report` (
  `id`            int NOT NULL AUTO_INCREMENT  COMMENT '举报ID',
  `uid`           int DEFAULT NULL             COMMENT '举报人ID，关联 db_account.id',
  `target_type`   varchar(255) DEFAULT NULL    COMMENT '举报目标类型：topic=帖子, comment=评论',
  `target_id`     int DEFAULT NULL             COMMENT '举报目标ID',
  `reason`        varchar(255) DEFAULT NULL    COMMENT '举报原因分类',
  `detail`        varchar(500) DEFAULT NULL    COMMENT '补充说明',
  `status`        varchar(255) DEFAULT 'pending' COMMENT '状态：pending=待处理, resolved=已处理, dismissed=已驳回',
  `admin_id`      int DEFAULT NULL             COMMENT '处理人ID',
  `admin_note`    varchar(500) DEFAULT NULL    COMMENT '处理备注',
  `resolve_time`  datetime DEFAULT NULL        COMMENT '处理时间',
  `time`          datetime DEFAULT NULL        COMMENT '举报时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报记录';

COMMIT;

-- ----------------------------
-- 图片存储记录表
-- ----------------------------
DROP TABLE IF EXISTS `db_image_store`;
CREATE TABLE `db_image_store` (
  `uid`  int DEFAULT NULL          COMMENT '上传用户ID，关联 db_account.id',
  `name` varchar(255) DEFAULT NULL COMMENT '图片在MinIO中的存储路径',
  `time` datetime DEFAULT NULL     COMMENT '上传时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图片存储记录表，记录上传到MinIO的图片信息';

SET FOREIGN_KEY_CHECKS = 1;
