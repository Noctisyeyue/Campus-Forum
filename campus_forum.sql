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
-- 变更：增加 status 字段（active/disabled）
-- ----------------------------
DROP TABLE IF EXISTS `db_account`;
CREATE TABLE `db_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT 'user',
  `status` varchar(255) DEFAULT 'active',
  `avatar` varchar(255) DEFAULT NULL,
  `register_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  `id` int NOT NULL,
  `gender` tinyint DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `qq` varchar(255) DEFAULT NULL,
  `wx` varchar(255) DEFAULT NULL,
  `desc` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

BEGIN;
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES (1, 0, NULL, NULL, NULL, '系统管理员');
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES (2, 0, NULL, NULL, NULL, '测试用户');
COMMIT;

-- ----------------------------
-- 用户隐私设置表
-- ----------------------------
DROP TABLE IF EXISTS `db_account_privacy`;
CREATE TABLE `db_account_privacy` (
  `id` int NOT NULL,
  `phone` tinyint DEFAULT 0,
  `email` tinyint DEFAULT 0,
  `wx` tinyint DEFAULT 0,
  `qq` tinyint DEFAULT 0,
  `gender` tinyint DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

BEGIN;
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES (1, 0, 0, 0, 0, 0);
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES (2, 0, 1, 0, 1, 1);
COMMIT;

-- ----------------------------
-- 帖子分类表
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_type`;
CREATE TABLE `db_topic_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `desc` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

BEGIN;
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (1, '日常闲聊', '在这里分享你的各种日常', '#1E90FF');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (2, '真诚交友', '在校园里寻找与自己志同道合的朋友', '#CE1EFF');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (3, '问题反馈', '反馈你在校园里遇到的问题', '#E07373');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (4, '恋爱官宣', '向大家展示你的恋爱成果', '#E0CE73');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (5, '踩坑记录', '将你遇到的坑分享给大家，防止其他人再次入坑', '#3BB62A');
COMMIT;

-- ----------------------------
-- 帖子表
-- 变更：增加 status、review_time、review_by、review_reason、last_submit_time、deleted_time、deleted_by
-- 帖子状态：pending_review / published / rejected / hidden / deleted
-- ----------------------------
DROP TABLE IF EXISTS `db_topic`;
CREATE TABLE `db_topic` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `content` text,
  `uid` int DEFAULT NULL,
  `type` int DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  `top` tinyint DEFAULT 0,
  `status` varchar(255) DEFAULT 'pending_review',
  `review_time` datetime DEFAULT NULL,
  `review_by` int DEFAULT NULL,
  `review_reason` varchar(255) DEFAULT NULL,
  `last_submit_time` datetime DEFAULT NULL,
  `deleted_time` datetime DEFAULT NULL,
  `deleted_by` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- 评论表
-- 变更：增加 status 字段（normal / deleted）
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_comment`;
CREATE TABLE `db_topic_comment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int DEFAULT NULL,
  `tid` int DEFAULT NULL,
  `content` text,
  `time` datetime DEFAULT NULL,
  `quote` int DEFAULT NULL,
  `status` varchar(255) DEFAULT 'normal',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- 点赞记录表
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_interact_like`;
CREATE TABLE `db_topic_interact_like` (
  `tid` int DEFAULT NULL,
  `uid` int DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  UNIQUE KEY `tid_uid_like` (`tid`, `uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- 收藏记录表
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_interact_collect`;
CREATE TABLE `db_topic_interact_collect` (
  `tid` int DEFAULT NULL,
  `uid` int DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  UNIQUE KEY `tid_uid_collect` (`tid`, `uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- 通知表
-- ----------------------------
DROP TABLE IF EXISTS `db_notification`;
CREATE TABLE `db_notification` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- 图片存储表
-- ----------------------------
DROP TABLE IF EXISTS `db_image_store`;
CREATE TABLE `db_image_store` (
  `uid` int DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
