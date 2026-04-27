package com.campus.forum.utils;

/**
 * 全局常量定义类
 */
public final class Const {

    private Const() {
    }

    // JWT 令牌相关
    public final static String JWT_BLACK_LIST = "jwt:blacklist:";      // JWT 黑名单 Key 前缀，后接令牌 UUID
    public final static String JWT_FREQUENCY = "jwt:frequency:";       // JWT 生成频率限制 Key 前缀，后接用户 ID

    // 请求频率限制相关
    public final static String FLOW_LIMIT_COUNTER = "flow:counter:";   // 请求计数器 Key 前缀
    public final static String FLOW_LIMIT_BLOCK = "flow:block:";       // 请求封禁标记 Key 前缀

    // 邮件验证码相关
    public final static String VERIFY_EMAIL_LIMIT = "verify:email:limit:"; // 验证码发送冷却 Key，后接邮箱
    public final static String VERIFY_EMAIL_DATA = "verify:email:data:";   // 验证码数据 Key，后接邮箱

    // 过滤器优先级
    public final static int ORDER_FLOW_LIMIT = -101;   // 限流过滤器优先级
    public final static int ORDER_CORS = -102;         // 跨域过滤器优先级

    // 请求自定义属性
    public final static String ATTR_USER_ID = "userId"; // 请求属性名：已认证用户 ID

    // 消息队列
    public final static String MQ_MAIL = "mail";        // RabbitMQ 邮件队列名称

    // 用户角色
    public final static String ROLE_DEFAULT = "user";   // 普通用户角色
    public final static String ROLE_ADMIN = "admin";    // 管理员角色

    // 论坛缓存与计数
    public final static String FORUM_WEATHER_CACHE = "weather:cache:";              // 天气缓存 Key
    public final static String FORUM_IMAGE_COUNTER = "forum:image:";                // 图片上传计数 Key，后接用户 ID
    public final static String FORUM_TOPIC_CREATE_COUNTER = "forum:topic:create:";  // 发帖计数 Key，后接用户 ID
    public final static String FORUM_TOPIC_COMMENT_COUNTER = "forum:topic:comment:";// 评论计数 Key，后接用户 ID
    public final static String FORUM_TOPIC_PREVIEW_CACHE = "topic:preview:";        // 帖子列表缓存 Key，后接页码

    // 帖子状态
    public final static String TOPIC_STATUS_PENDING = "pending_review"; // 待审核
    public final static String TOPIC_STATUS_PUBLISHED = "published";    // 已发布
    public final static String TOPIC_STATUS_REJECTED = "rejected";      // 已拒绝
    public final static String TOPIC_STATUS_HIDDEN = "hidden";          // 已隐藏（管理员下架）
    public final static String TOPIC_STATUS_DELETED = "deleted";        // 已删除（软删除）

    // 评论状态
    public final static String COMMENT_STATUS_NORMAL = "normal";        // 评论正常
    public final static String COMMENT_STATUS_DELETED = "deleted";      // 评论已删除（软删除）

    // 举报状态
    public final static String REPORT_STATUS_PENDING = "pending";       // 待处理
    public final static String REPORT_STATUS_RESOLVED = "resolved";     // 已处理
    public final static String REPORT_STATUS_DISMISSED = "dismissed";   // 已驳回

    // 举报目标类型
    public final static String REPORT_TARGET_TOPIC = "topic";
    public final static String REPORT_TARGET_COMMENT = "comment";
}
