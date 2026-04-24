-- =============================================
-- Campus Forum 测试数据（仅 INSERT）
-- 使用前请先导入 campus_forum.sql 建好表结构
-- 所有用户密码均为 123456
-- =============================================

USE `campus_forum`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 清空所有表数据
-- ----------------------------
DELETE FROM `db_topic_interact_like`;
DELETE FROM `db_topic_interact_collect`;
DELETE FROM `db_topic_comment`;
DELETE FROM `db_notification`;
DELETE FROM `db_image_store`;
DELETE FROM `db_topic`;
DELETE FROM `db_account_privacy`;
DELETE FROM `db_account_details`;
DELETE FROM `db_account`;

-- ----------------------------
-- 用户账户（7人：1管理员 + 5正常用户 + 1禁用用户）
-- ----------------------------
BEGIN;
INSERT INTO `db_account` (`id`, `username`, `password`, `email`, `role`, `status`, `avatar`, `register_time`) VALUES
(1, 'admin', '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'admin@campus.com',  'admin', 'active',   NULL, '2025-08-01 08:00:00'),
(2, 'test',  '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'test@campus.com',   'user',  'active',   NULL, '2025-08-27 00:18:20'),
(3, '张三',  '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'zhangsan@163.com',  'user',  'active',   NULL, '2025-09-10 10:30:00'),
(4, '李四',  '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'lisi@qq.com',       'user',  'active',   NULL, '2025-09-15 14:20:00'),
(5, '王五',  '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'wangwu@163.com',    'user',  'active',   NULL, '2025-10-01 09:00:00'),
(6, '赵六',  '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'zhaoliu@qq.com',    'user',  'disabled', NULL, '2025-10-20 16:45:00'),
(7, '孙七',  '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', 'sunqi@163.com',     'user',  'active',   NULL, '2025-11-05 11:15:00');
COMMIT;

-- ----------------------------
-- 用户详情
-- ----------------------------
BEGIN;
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES
(1, 0, NULL,        NULL,         NULL,         '系统管理员，负责论坛日常管理'),
(2, 1, '13800000002', '123456789', 'test_wx',    '热爱编程的测试用户'),
(3, 1, '13800000003', '987654321', 'zhangsan_wx', '计科大三，喜欢打篮球'),
(4, 2, '13800000004', '112233445', 'lisi_wx',     '软件工程大二，爱好摄影'),
(5, 1, '13800000005', '556677889', 'wangwu_wx',   '数媒大三，会剪辑会拍照'),
(6, 1, '13800000006', '998877665', 'zhaoliu_wx',  '已被禁用的用户'),
(7, 2, '13800000007', '334455667', 'sunqi_wx',    '刚注册的新用户，请多关照');
COMMIT;

-- ----------------------------
-- 隐私设置
-- ----------------------------
BEGIN;
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES
(1, 0, 0, 0, 0, 0),
(2, 1, 1, 0, 1, 1),
(3, 0, 1, 1, 1, 1),
(4, 0, 0, 0, 1, 1),
(5, 1, 1, 1, 1, 1),
(6, 0, 0, 0, 0, 0),
(7, 0, 1, 0, 0, 1);
COMMIT;

-- ----------------------------
-- 帖子（15篇：覆盖所有状态）
-- published=10, pending_review=2, rejected=1, hidden=1, deleted=1
-- ----------------------------
BEGIN;
-- 置顶公告
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(1, '欢迎大家来到Campus Forum校园论坛', '{"ops":[{"insert":"大家好！欢迎来到 Campus Forum 校园论坛系统，这里是属于我们学生的交流空间。\\n在这里你可以：\\n1. 分享校园生活的点点滴滴\\n2. 寻找志同道合的朋友\\n3. 反馈校园生活中遇到的问题\\n4. 记录踩坑经验，帮助他人避坑\\n\\n请遵守社区规范，文明发言哦！\\n"}]}',
 1, 1, '2025-08-01 08:30:00', 1, 'published', '2025-08-01 08:31:00', 1, NULL, '2025-08-01 08:30:00', NULL, NULL);

INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(2, '社区规范公告——请所有用户必读', '{"ops":[{"insert":"为了维护良好的社区氛围，请大家遵守以下规范：\\n\\n1. 禁止发布违法、违规内容\\n2. 禁止人身攻击和恶意引战\\n3. 禁止广告和垃圾信息\\n4. 尊重他人隐私，不随意泄露他人信息\\n5. 鼓励友善交流，互帮互助\\n\\n违反规范的用户将被警告或禁用账号。\\n"}]}',
 1, 3, '2025-08-01 09:00:00', 1, 'published', '2025-08-01 09:01:00', 1, NULL, '2025-08-01 09:00:00', NULL, NULL);

-- 日常闲聊
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(3, '食堂二楼新开的麻辣烫太好吃了', '{"ops":[{"insert":"强烈推荐！今天中午去食堂二楼尝了新开的麻辣烫窗口，味道绝了！\\n\\n汤底可以选择麻辣、番茄、菌菇三种，食材也很新鲜。我选了麻辣汤底，加了牛肉卷、豆皮、土豆粉、藕片，一共才15块钱，吃得饱饱的。\\n\\n唯一缺点就是排队人太多了，建议避开12点高峰期。\\n"}]}',
 3, 1, '2025-10-15 12:30:00', 0, 'published', '2025-10-15 12:31:00', 1, NULL, '2025-10-15 12:30:00', NULL, NULL);

-- 踩坑记录
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(4, '千万别在闲鱼上买到手刀的二手手机', '{"ops":[{"insert":"血泪教训分享给大家！\\n\\n上周在闲鱼上买了一台二手iPhone，卖家说95新无拆修，结果收到货发现屏幕有划痕，电池健康度只有78%。找卖家理论，对方直接已读不回。\\n\\n后来找闲鱼客服投诉，折腾了一周才退款。建议大家买二手电子产品一定要：\\n1. 要求卖家拍详细视频\\n2. 走验机服务\\n3. 不要贪便宜，价格低得离谱的一定有问题\\n"}]}',
 4, 5, '2025-10-18 15:20:00', 0, 'published', '2025-10-18 15:21:00', 1, NULL, '2025-10-18 15:20:00', NULL, NULL);

-- 真诚交友
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(5, '有没有一起打羽毛球的同学', '{"ops":[{"insert":"本人计科大三男生，羽毛球爱好者，水平业余中等。\\n\\n平时工作日晚上或者周末都有空，学校体育馆的场地费也不贵。有没有想一起打球的同学？可以加我微信，拉个群约球。\\n\\n不限男女，不限水平，主打一个快乐运动！\\n"}]}',
 3, 2, '2025-10-20 19:00:00', 0, 'published', '2025-10-20 19:01:00', 1, NULL, '2025-10-20 19:00:00', NULL, NULL);

-- 问题反馈
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(6, '图书馆三楼空调坏了快两周了', '{"ops":[{"insert":"如题，图书馆三楼靠窗那一片的空调从国庆前就坏了，到现在都没修好。每天下午坐在那里学习热得不行，只能靠自带小风扇续命。\\n\\n有没有同学知道找哪个部门反映比较有效？我已经在后勤系统提交了工单但是没人理。\\n"}]}',
 5, 3, '2025-10-22 14:10:00', 0, 'published', '2025-10-22 14:11:00', 1, NULL, '2025-10-22 14:10:00', NULL, NULL);

-- 恋爱官宣
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(7, '在一起1000天纪念', '{"ops":[{"insert":"今天是我和她在一起的第1000天！从大一军训时相识，到现在大三，一起走过了很多。\\n\\n感谢你陪我熬夜复习、陪我吃遍学校周边的小吃、在我低落的时候给我鼓励。\\n\\n未来的日子，继续一起走下去吧！\\n"}]}',
 4, 4, '2025-10-25 20:30:00', 0, 'published', '2025-10-25 20:31:00', 1, NULL, '2025-10-25 20:30:00', NULL, NULL);

-- 日常闲聊
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(8, '推荐几部最近在看的好剧', '{"ops":[{"insert":"最近剧荒的看过来！推荐几部个人觉得很好看的：\\n\\n1. 《漫长的季节》——悬疑神剧，范伟演技炸裂\\n2. 《狂飙》——年初的现象级作品，应该都看过了\\n3. 《三体》——虽然有争议但整体质量不错\\n4. 《去有风的地方》——治愈系，适合放松的时候看\\n\\n大家有什么好看的也可以推荐给我！\\n"}]}',
 5, 1, '2025-10-28 21:15:00', 0, 'published', '2025-10-28 21:16:00', 1, NULL, '2025-10-28 21:15:00', NULL, NULL);

-- 新人报到
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(9, '新人报道，请多指教', '{"ops":[{"insert":"大家好！我是一名大二学生，今天刚注册论坛。\\n\\n专业是软件工程，对前端开发比较感兴趣，目前正在学 Vue3。希望在这里认识更多志同道合的同学，一起学习进步！\\n"}]}',
 7, 1, '2025-11-05 11:30:00', 0, 'published', '2025-11-05 11:31:00', 1, NULL, '2025-11-05 11:30:00', NULL, NULL);

-- 期末复习
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(15, '期末复习有没有什么好方法', '{"ops":[{"insert":"马上就要期末考试了，有没有学霸分享一下复习方法？\\n\\n特别是高数和线代，感觉每次复习都很低效，看了一整天书结果什么都没记住。\\n\\n求大佬指点！\\n"}]}',
 2, 3, '2025-11-04 20:00:00', 0, 'published', '2025-11-04 20:01:00', 1, NULL, '2025-11-04 20:00:00', NULL, NULL);

-- 待审核帖子
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(10, '有没有人想组队参加下周的编程比赛', '{"ops":[{"insert":"下周六学校有一个编程比赛，好像是ACM校赛的选拔。\\n\\n本人会一点算法但不算强，想找1-2个队友一起组队。最好熟悉数据结构和基础算法，我们三个人一起刷题备赛。\\n\\n有兴趣的同学私信我！\\n"}]}',
 2, 2, '2025-11-06 10:00:00', 0, 'pending_review', NULL, NULL, NULL, '2025-11-06 10:00:00', NULL, NULL);

INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(11, '学校周边有什么好吃的烧烤推荐吗', '{"ops":[{"insert":"如题，最近特别馋烧烤，但是不知道学校附近哪家好吃。\\n\\n有没有吃过的同学推荐一下？最好人均50以内的。\\n"}]}',
 7, 1, '2025-11-06 14:30:00', 0, 'pending_review', NULL, NULL, NULL, '2025-11-06 14:30:00', NULL, NULL);

-- 已拒绝
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(12, '二手教材转让群加群链接', '{"ops":[{"insert":"加群链接：xxx\\n"}]}',
 6, 1, '2025-11-01 09:00:00', 0, 'rejected', '2025-11-01 09:30:00', 1, '帖子内容过少，且疑似广告链接，请补充详细描述后重新提交', '2025-11-01 09:00:00', NULL, NULL);

-- 已隐藏
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(13, '关于某老师的课程评价', '{"ops":[{"insert":"这门课说实话体验一般，老师讲得太快了，PPT也跟不上。不过考试倒是不难，认真复习就能过。\\n"}]}',
 3, 3, '2025-10-30 16:00:00', 0, 'hidden', '2025-11-02 10:00:00', 1, '涉及具体教师评价，为避免争议暂时隐藏', '2025-10-30 16:00:00', NULL, NULL);

-- 已删除（软删除）
INSERT INTO `db_topic` (`id`, `title`, `content`, `uid`, `type`, `time`, `top`, `status`, `review_time`, `review_by`, `review_reason`, `last_submit_time`, `deleted_time`, `deleted_by`) VALUES
(14, '测试帖子已被删除', '{"ops":[{"insert":"这是一条测试内容\\n"}]}',
 2, 1, '2025-10-10 08:00:00', 0, 'deleted', '2025-10-10 08:05:00', 1, NULL, '2025-10-10 08:00:00', '2025-11-03 15:00:00', 1);
COMMIT;

-- ----------------------------
-- 评论（20条：覆盖顶级评论、回复评论）
-- ----------------------------
BEGIN;
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`, `status`) VALUES
(1,  2, 3,  '{"ops":[{"insert":"我也吃了！确实好吃，番茄汤底也很推荐\\n"}]}',                                          '2025-10-15 13:00:00', -1, 'normal'),
(2,  4, 3,  '{"ops":[{"insert":"排队真的太长了，等了半个小时\\n"}]}',                                                  '2025-10-15 13:05:00', -1, 'normal'),
(3,  3, 3,  '{"ops":[{"insert":"回复二楼：工作日上午去基本不用排队\\n"}]}',                                              '2025-10-15 14:00:00', 2,  'normal'),
(4,  5, 4,  '{"ops":[{"insert":"我也踩过类似的坑，二手水太深了\\n"}]}',                                                  '2025-10-18 16:00:00', -1, 'normal'),
(5,  3, 4,  '{"ops":[{"insert":"买二手走验机是真的重要，多花几十块钱但安心很多\\n"}]}',                                    '2025-10-18 16:30:00', 4,  'normal'),
(6,  2, 4,  '{"ops":[{"insert":"学习到了，以后买二手一定注意\\n"}]}',                                                    '2025-10-18 17:00:00', -1, 'normal'),
(7,  5, 5,  '{"ops":[{"insert":"我可以！周几打球？我加你微信\\n"}]}',                                                    '2025-10-20 19:30:00', -1, 'normal'),
(8,  7, 5,  '{"ops":[{"insert":"新手可以吗？我只会基础的\\n"}]}',                                                        '2025-10-20 20:00:00', -1, 'normal'),
(9,  3, 5,  '{"ops":[{"insert":"回复8楼：当然可以，大家一起玩嘛\\n"}]}',                                                  '2025-10-20 20:15:00', 8,  'normal'),
(10, 2, 6,  '{"ops":[{"insert":"同感！三楼热得要命，我改去一楼了\\n"}]}',                                                '2025-10-22 15:00:00', -1, 'normal'),
(11, 4, 6,  '{"ops":[{"insert":"可以试试找辅导员反映，比后勤系统管用\\n"}]}',                                              '2025-10-22 15:30:00', -1, 'normal'),
(12, 3, 6,  '{"ops":[{"insert":"已经在学生会那边反馈了，说是下周安排维修\\n"}]}',                                          '2025-10-23 09:00:00', 10, 'normal'),
(13, 2, 7,  '{"ops":[{"insert":"祝你们一直幸福下去！\\n"}]}',                                                            '2025-10-25 21:00:00', -1, 'normal'),
(14, 5, 7,  '{"ops":[{"insert":"酸了酸了，单身狗默默路过\\n"}]}',                                                        '2025-10-25 21:30:00', -1, 'normal'),
(15, 3, 8,  '{"ops":[{"insert":"漫长的季节确实好看！范伟太强了\\n"}]}',                                                  '2025-10-28 22:00:00', -1, 'normal'),
(16, 7, 8,  '{"ops":[{"insert":"推荐《莲花楼》，古装悬疑也很好看\\n"}]}',                                                '2025-10-28 22:30:00', -1, 'normal'),
(17, 3, 15, '{"ops":[{"insert":"高数建议刷历年真题，题型基本固定\\n"}]}',                                                '2025-11-04 21:00:00', -1, 'normal'),
(18, 4, 15, '{"ops":[{"insert":"线代推荐3Blue1Brown的视频，看完就理解了\\n"}]}',                                          '2025-11-04 21:30:00', -1, 'normal'),
(19, 2, 15, '{"ops":[{"insert":"回复18楼：那个视频确实不错，我看完受益匪浅\\n"}]}',                                        '2025-11-04 22:00:00', 18, 'normal'),
(20, 2, 9,  '{"ops":[{"insert":"欢迎新同学！\\n"}]}',                                                                    '2025-11-05 12:00:00', -1, 'normal');
COMMIT;

-- ----------------------------
-- 点赞记录
-- ----------------------------
BEGIN;
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES
(1,  2, '2025-08-02 10:00:00'),
(1,  3, '2025-08-03 14:00:00'),
(1,  4, '2025-08-05 09:00:00'),
(3,  2, '2025-10-15 14:00:00'),
(3,  4, '2025-10-16 10:00:00'),
(3,  5, '2025-10-17 11:00:00'),
(4,  3, '2025-10-18 16:00:00'),
(4,  5, '2025-10-19 09:00:00'),
(5,  5, '2025-10-20 19:30:00'),
(5,  7, '2025-10-21 10:00:00'),
(6,  2, '2025-10-22 15:00:00'),
(6,  3, '2025-10-23 09:00:00'),
(7,  2, '2025-10-25 21:00:00'),
(7,  3, '2025-10-26 08:00:00'),
(7,  5, '2025-10-26 12:00:00'),
(8,  3, '2025-10-28 22:00:00'),
(8,  4, '2025-10-29 10:00:00'),
(15, 3, '2025-11-04 21:00:00'),
(15, 4, '2025-11-04 22:00:00'),
(15, 7, '2025-11-05 09:00:00');
COMMIT;

-- ----------------------------
-- 收藏记录
-- ----------------------------
BEGIN;
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES
(1,  2, '2025-08-02 10:00:00'),
(1,  3, '2025-08-05 15:00:00'),
(4,  3, '2025-10-18 16:30:00'),
(4,  5, '2025-10-20 08:00:00'),
(8,  3, '2025-10-29 09:00:00'),
(8,  7, '2025-10-30 11:00:00'),
(15, 2, '2025-11-05 10:00:00'),
(15, 3, '2025-11-05 12:00:00');
COMMIT;

-- ----------------------------
-- 通知（6条：审核通过/拒绝/隐藏/评论回复）
-- ----------------------------
BEGIN;
INSERT INTO `db_notification` (`id`, `uid`, `title`, `content`, `type`, `url`, `time`) VALUES
(1, 3, '帖子审核通过', '您的帖子《食堂二楼新开的麻辣烫太好吃了》已通过审核',                                                 'topic_review',   '/index/topic-detail/3',  '2025-10-15 12:31:00'),
(2, 4, '帖子审核通过', '您的帖子《千万别在闲鱼上买到手刀的二手手机》已通过审核',                                              'topic_review',   '/index/topic-detail/4',  '2025-10-18 15:21:00'),
(3, 6, '帖子审核被拒绝', '您的帖子《二手教材转让群加群链接》审核未通过，原因：帖子内容过少，且疑似广告链接',                      'topic_review',   NULL,                    '2025-11-01 09:30:00'),
(4, 3, '帖子已被隐藏', '您的帖子《关于某老师的课程评价》已被管理员隐藏，原因：涉及具体教师评价，为避免争议暂时隐藏',             'topic_review',   NULL,                    '2025-11-02 10:00:00'),
(5, 2, '收到评论回复', '张三 回复了你在《食堂二楼新开的麻辣烫太好吃了》中的评论',                                              'comment_reply',  '/index/topic-detail/3',  '2025-10-15 14:00:00'),
(6, 2, '收到评论回复', 'test 回复了你在《期末复习有没有什么好方法》中的评论',                                                   'comment_reply',  '/index/topic-detail/15', '2025-11-04 22:00:00');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
