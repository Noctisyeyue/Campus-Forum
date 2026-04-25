p[-- =============================================
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
  `id`            int NOT NULL AUTO_INCREMENT    COMMENT '用户ID，主键自增',
  `username`      varchar(255) DEFAULT NULL      COMMENT '用户名',
  `password`      varchar(255) DEFAULT NULL      COMMENT '密码（BCrypt加密存储）',
  `email`         varchar(255) DEFAULT NULL      COMMENT '邮箱地址，用于注册和找回密码',
  `role`          varchar(255) DEFAULT 'user'    COMMENT '角色：user=普通用户, admin=管理员',
  `status`        varchar(255) DEFAULT 'active'  COMMENT '账号状态：active=正常, disabled=禁用',
  `avatar`        varchar(255) DEFAULT NULL      COMMENT '头像路径（MinIO存储地址）',
  `register_time` datetime DEFAULT NULL          COMMENT '注册时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户表，存储用户的基本登录信息';

BEGIN;
-- 管理员账号：admin / 123456
INSERT INTO `db_account` (`id`, `username`, `password`, `email`, `role`, `status`, `avatar`, `register_time`) VALUES
(1, 'admin', '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'admin@campus.com', 'admin', 'active', NULL, '2023-08-27 00:00:00');
-- 测试用户：test / 123456
INSERT INTO `db_account` (`id`, `username`, `password`, `email`, `role`, `status`, `avatar`, `register_time`) VALUES
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
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES (1, 0, NULL, NULL, NULL, '系统管理员');
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES (2, 0, NULL, NULL, NULL, '测试用户');
COMMIT;

-- ----------------------------
-- 用户隐私设置表
-- ----------------------------
DROP TABLE IF EXISTS `db_account_privacy`;
CREATE TABLE `db_account_privacy` (
  `id`     int NOT NULL          COMMENT '用户ID，与 db_account.id 一致',
  `phone`  tinyint DEFAULT 0     COMMENT '手机号是否公开：0=隐藏, 1=公开',
  `email`  tinyint DEFAULT 0     COMMENT '邮箱是否公开：0=隐藏, 1=公开',
  `wx`     tinyint DEFAULT 0     COMMENT '微信是否公开：0=隐藏, 1=公开',
  `qq`     tinyint DEFAULT 0     COMMENT 'QQ是否公开：0=隐藏, 1=公开',
  `gender` tinyint DEFAULT 0     COMMENT '性别是否公开：0=隐藏, 1=公开',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户隐私设置表，控制用户个人资料中哪些信息对外公开';

BEGIN;
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES (1, 0, 0, 0, 0, 0);
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES (2, 0, 1, 0, 1, 1);
COMMIT;

-- ----------------------------
-- 帖子分类表
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_type`;
CREATE TABLE `db_topic_type` (
  `id`    int NOT NULL AUTO_INCREMENT  COMMENT '分类ID，主键自增',
  `name`  varchar(255) DEFAULT NULL    COMMENT '分类名称（如：日常闲聊）',
  `desc`  varchar(255) DEFAULT NULL    COMMENT '分类描述',
  `color` varchar(255) DEFAULT NULL    COMMENT '标签颜色（十六进制色值，如：#1E90FF）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子分类表，定义论坛的帖子分类，由管理员在后台维护';

BEGIN;
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (1, '日常闲聊', '在这里分享你的各种日常', '#1E90FF');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (2, '真诚交友', '在校园里寻找与自己志同道合的朋友', '#CE1EFF');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (3, '问题反馈', '反馈你在校园里遇到的问题', '#E07373');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (4, '恋爱官宣', '向大家展示你的恋爱成果', '#E0CE73');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (5, '踩坑记录', '将你遇到的坑分享给大家，防止其他人再次入坑', '#3BB62A');
COMMIT;

-- ----------------------------
-- 帖子表
-- 帖子生命周期：pending_review → published / rejected / hidden / deleted
-- ----------------------------
DROP TABLE IF EXISTS `db_topic`;
CREATE TABLE `db_topic` (
  `id`               int NOT NULL AUTO_INCREMENT          COMMENT '帖子ID，主键自增',
  `title`            varchar(255) DEFAULT NULL             COMMENT '帖子标题',
  `content`          text DEFAULT NULL                     COMMENT '帖子内容（JSON格式，Quill富文本编辑器的Delta数据）',
  `uid`              int DEFAULT NULL                      COMMENT '发帖用户ID，关联 db_account.id',
  `type`             int DEFAULT NULL                      COMMENT '分类ID，关联 db_topic_type.id',
  `time`             datetime DEFAULT NULL                 COMMENT '创建时间',
  `top`              tinyint DEFAULT 0                     COMMENT '是否置顶：0=否, 1=是',
  `status`           varchar(255) DEFAULT 'pending_review' COMMENT '帖子状态：pending_review=待审核, published=已发布, rejected=已拒绝, hidden=已隐藏, deleted=已删除',
  `review_time`      datetime DEFAULT NULL                 COMMENT '最近一次审核时间',
  `review_by`        int DEFAULT NULL                      COMMENT '审核人ID，关联 db_account.id（哪个管理员审核的）',
  `review_reason`    varchar(255) DEFAULT NULL             COMMENT '审核理由（如拒绝原因）',
  `last_submit_time` datetime DEFAULT NULL                 COMMENT '最后提交审核时间（编辑后重新提交会更新）',
  `deleted_time`     datetime DEFAULT NULL                 COMMENT '删除时间（软删除）',
  `deleted_by`       int DEFAULT NULL                      COMMENT '删除人ID，关联 db_account.id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子表，核心业务表，存储论坛帖子内容和审核状态';

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
  `quote`   int DEFAULT NULL                     COMMENT '引用的评论ID：-1=顶级评论（直接评论帖子）, 其他值=回复的评论ID',
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表，存储系统通知（如帖子审核结果、评论回复提醒等）';

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
