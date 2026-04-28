package com.campus.forum.utils;

/**
 * 全局常量定义类
 */
public final class Const {

    private Const() {
    }

    /** JWT 黑名单 Key 前缀，后接令牌 UUID */
    public final static String JWT_BLACK_LIST = "jwt:blacklist:";
    /** JWT 生成频率限制 Key 前缀，后接用户 ID */
    public final static String JWT_FREQUENCY = "jwt:frequency:";

    /** 请求计数器 Key 前缀，用于限流统计 */
    public final static String FLOW_LIMIT_COUNTER = "flow:counter:";
    /** 请求封禁标记 Key 前缀，触发限流后标记 */
    public final static String FLOW_LIMIT_BLOCK = "flow:block:";

    /** 验证码发送冷却 Key，后接邮箱，防止频繁发送 */
    public final static String VERIFY_EMAIL_LIMIT = "verify:email:limit:";
    /** 验证码数据 Key，后接邮箱，存储实际验证码 */
    public final static String VERIFY_EMAIL_DATA = "verify:email:data:";

    /** 限流过滤器优先级，值越小优先级越高 */
    public final static int ORDER_FLOW_LIMIT = -101;
    /** 跨域过滤器优先级，值越小优先级越高 */
    public final static int ORDER_CORS = -102;

    /** 请求属性名：已认证用户 ID，由 JWT 过滤器注入 */
    public final static String ATTR_USER_ID = "userId";

    /** RabbitMQ 邮件队列名称 */
    public final static String MQ_MAIL = "mail";

    /** 普通用户角色标识 */
    public final static String ROLE_DEFAULT = "user";
    /** 管理员角色标识 */
    public final static String ROLE_ADMIN = "admin";

    /** 天气接口缓存 Key */
    public final static String FORUM_WEATHER_CACHE = "weather:cache:";
    /** 图片上传计数 Key，后接用户 ID，用于限制上传频率 */
    public final static String FORUM_IMAGE_COUNTER = "forum:image:";
    /** 发帖计数 Key，后接用户 ID，用于限制发帖频率 */
    public final static String FORUM_TOPIC_CREATE_COUNTER = "forum:topic:create:";
    /** 评论计数 Key，后接用户 ID，用于限制评论频率 */
    public final static String FORUM_TOPIC_COMMENT_COUNTER = "forum:topic:comment:";
    /** 帖子列表缓存 Key，后接页码和筛选条件 */
    public final static String FORUM_TOPIC_PREVIEW_CACHE = "topic:preview:";

    /** 帖子状态：待审核，新发帖或编辑后的初始状态 */
    public final static String TOPIC_STATUS_PENDING = "pending_review";
    /** 帖子状态：已发布，审核通过后对用户可见 */
    public final static String TOPIC_STATUS_PUBLISHED = "published";
    /** 帖子状态：已拒绝，审核未通过，附拒绝原因 */
    public final static String TOPIC_STATUS_REJECTED = "rejected";
    /** 帖子状态：已下架，管理员手动下架，前台不可见但后台可恢复 */
    public final static String TOPIC_STATUS_HIDDEN = "hidden";
    /** 帖子状态：已删除，用户软删除或管理员物理删除 */
    public final static String TOPIC_STATUS_DELETED = "deleted";

    /** 评论状态：正常，默认状态 */
    public final static String COMMENT_STATUS_NORMAL = "normal";
    /** 评论状态：已删除，软删除标记 */
    public final static String COMMENT_STATUS_DELETED = "deleted";

    /** 举报状态：待处理，用户新提交的举报 */
    public final static String REPORT_STATUS_PENDING = "pending";
    /** 举报状态：已处理，管理员确认违规并下架/删除内容 */
    public final static String REPORT_STATUS_RESOLVED = "resolved";
    /** 举报状态：已驳回，管理员认为举报不成立 */
    public final static String REPORT_STATUS_DISMISSED = "dismissed";

    /** 举报目标类型：帖子 */
    public final static String REPORT_TARGET_TOPIC = "topic";
    /** 举报目标类型：评论 */
    public final static String REPORT_TARGET_COMMENT = "comment";
}
