package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Interact;
import com.campus.forum.entity.dto.Topic;
import com.campus.forum.entity.dto.TopicType;
import com.campus.forum.entity.vo.request.AddCommentVO;
import com.campus.forum.entity.vo.request.ForumNoticeSaveVO;
import com.campus.forum.entity.vo.request.PublishActivityVO;
import com.campus.forum.entity.vo.request.PublishNoticeTopicVO;
import com.campus.forum.entity.vo.request.TopicCreateVO;
import com.campus.forum.entity.vo.request.TopicUpdateVO;
import com.campus.forum.entity.vo.response.PageResult;
import com.campus.forum.entity.vo.response.AdminCommentVO;
import com.campus.forum.entity.vo.response.AdminTopicVO;
import com.campus.forum.entity.vo.response.CommentVO;
import com.campus.forum.entity.vo.response.ForumNoticeVO;
import com.campus.forum.entity.vo.response.TopicDetailVO;
import com.campus.forum.entity.vo.response.TopicPreviewVO;
import com.campus.forum.entity.vo.response.TopicTopVO;
import com.campus.forum.entity.vo.response.UserTopicVO;

import java.util.List;

/**
 * 帖子服务，包含发帖审核、列表查询、评论、互动等核心业务
 */
public interface TopicService extends IService<Topic> {

    /**
     * 获取全部帖子分类
     *
     * @return 分类列表
     */
    List<TopicType> listTypes();

    /**
     * 创建新帖子
     *
     * @param uid 用户ID
     * @param vo  帖子创建参数
     * @return null 表示成功，非 null 为错误信息
     */
    String createTopic(int uid, TopicCreateVO vo);

    /**
     * 分页查询帖子列表（支持分类、排序、搜索）
     *
     * @param page  页码
     * @param type  分类ID（0=全部）
     * @param sort  排序方式
     * @param title 搜索标题关键词（可选）
     * @return 帖子预览列表
     */
    List<TopicPreviewVO> listTopicByPage(int page, int type, String sort, String title);

    /**
     * 分页查询活动帖子列表
     *
     * @param page  页码
     * @param title 搜索标题关键词（可选）
     * @return 帖子预览列表
     */
    List<TopicPreviewVO> listActivityByPage(int page, String title);

    /**
     * 分页查询教务通知帖子列表
     *
     * @param page  页码
     * @param title 搜索标题关键词（可选）
     * @return 帖子预览列表
     */
    List<TopicPreviewVO> listNoticeTopicByPage(int page, String title);

    /**
     * 分页查询指定用户的帖子列表
     *
     * @param uid    用户ID
     * @param page   页码
     * @param status 帖子状态筛选（可选）
     * @return 用户帖子列表
     */
    List<UserTopicVO> listUserTopics(int uid, int page, String status);

    /**
     * 获取置顶帖子列表
     *
     * @return 置顶帖子列表
     */
    List<TopicTopVO> listTopTopics();

    /**
     * 获取帖子详情（含互动状态）
     *
     * @param tid 帖子ID
     * @param uid 当前用户ID
     * @return 帖子详情
     */
    TopicDetailVO getTopic(int tid, int uid);

    /**
     * 获取自己的帖子详情（包含编辑权限信息）
     *
     * @param tid 帖子ID
     * @param uid 当前用户ID
     * @return 帖子详情
     */
    TopicDetailVO getOwnTopic(int tid, int uid);

    /**
     * 管理员获取帖子详情
     *
     * @param tid 帖子ID
     * @return 帖子详情
     */
    TopicDetailVO adminGetTopic(int tid);

    /**
     * 管理员获取自己发布的帖子详情（用于编辑回填，校验作者身份）
     *
     * @param tid     帖子ID
     * @param adminId 当前管理员ID
     * @return 帖子详情
     */
    TopicDetailVO adminGetOwnTopic(int tid, int adminId);

    /**
     * 获取论坛公告
     *
     * @return 论坛公告内容
     */
    ForumNoticeVO getForumNotice();

    /**
     * 切换互动状态（点赞/收藏）
     *
     * @param interact 互动信息
     * @param state    目标状态（true=操作，false=取消）
     */
    void interact(Interact interact, boolean state);

    /**
     * 获取用户收藏的帖子列表
     *
     * @param uid 用户ID
     * @return 收藏帖子列表
     */
    List<TopicPreviewVO> listTopicCollects(int uid);

    /**
     * 更新帖子内容
     *
     * @param uid 用户ID
     * @param vo  帖子更新参数
     * @return null 表示成功，非 null 为错误信息
     */
    String updateTopic(int uid, TopicUpdateVO vo);

    /**
     * 发表评论
     *
     * @param uid 用户ID
     * @param vo  评论参数
     * @return null 表示成功，非 null 为错误信息
     */
    String createComment(int uid, AddCommentVO vo);

    /**
     * 查询帖子评论列表（仅 normal 状态）
     *
     * @param tid        帖子ID
     * @param pageNumber 页码
     * @return 评论VO列表
     */
    List<CommentVO> comments(int tid, int pageNumber);

    /**
     * 软删除评论（用户端）
     *
     * @param id  评论ID
     * @param uid 用户ID
     * @return null 表示成功，非 null 为错误信息
     */
    String deleteComment(int id, int uid);

    /**
     * 软删除帖子（用户端）
     *
     * @param uid 用户ID
     * @param tid 帖子ID
     * @return null 表示成功，非 null 为错误信息
     */
    String deleteTopic(int uid, int tid);

    /**
     * 分页查询全部帖子（管理员，支持状态/分类/标题/作者筛选）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   帖子状态（可选）
     * @param type     分类ID（可选）
     * @param title    标题关键词（可选）
     * @param author   作者用户名（可选）
     * @return 分页结果
     */
    PageResult<AdminTopicVO> adminListTopics(int page, int pageSize, String status, Integer type, String title, String author);

    /**
     * 审核通过帖子
     *
     * @param tid     帖子ID
     * @param adminId 操作管理员ID
     */
    void adminApproveTopic(int tid, int adminId);

    /**
     * 审核拒绝帖子
     *
     * @param tid     帖子ID
     * @param adminId 操作管理员ID
     * @param reason  拒绝原因
     */
    void adminRejectTopic(int tid, int adminId, String reason);

    /**
     * 下架帖子
     *
     * @param tid    帖子ID
     * @param reason 下架原因
     * @return null 表示成功，非 null 为错误信息
     */
    String adminHideTopic(int tid, String reason);

    /**
     * 上架帖子（恢复已下架帖子）
     *
     * @param tid 帖子ID
     * @return null 表示成功，非 null 为错误信息
     */
    String adminRestoreTopic(int tid);

    /**
     * 删除帖子（物理删除，不可逆）
     *
     * @param tid     帖子ID
     * @param adminId 操作管理员ID
     * @return null 表示成功，非 null 为错误信息
     */
    String adminDeleteTopic(int tid, int adminId);

    /**
     * 置顶帖子，已置顶则返回错误信息
     *
     * @param tid 帖子ID
     * @return null 表示成功，否则返回错误信息
     */
    String adminTopTopic(int tid);

    /**
     * 取消置顶，未置顶则返回错误信息
     *
     * @param tid 帖子ID
     * @return null 表示成功，否则返回错误信息
     */
    String adminUntopTopic(int tid);

    /**
     * 管理员编辑自己发布的帖子（标题、正文，活动帖子可同步更新扩展字段）
     *
     * @param adminId 当前管理员ID
     * @param tid     帖子ID
     * @param title   新标题
     * @param content 新正文（JSON Delta）
     * @param vo      活动扩展数据（非活动帖子传 null）
     * @return null 表示成功，非 null 为错误信息
     */
    String adminUpdateTopic(int adminId, int tid, String title, String content, PublishActivityVO vo);

    /**
     * 发布校园活动
     *
     * @param adminId 操作管理员ID
     * @param vo      活动发布参数
     * @return null 表示成功，非 null 为错误信息
     */
    String publishActivity(int adminId, PublishActivityVO vo);

    /**
     * 发布教务通知
     *
     * @param adminId 操作管理员ID
     * @param vo      通知发布参数
     * @return null 表示成功，非 null 为错误信息
     */
    String publishNoticeTopic(int adminId, PublishNoticeTopicVO vo);

    /**
     * 保存论坛公告
     *
     * @param adminId 操作管理员ID
     * @param vo      公告保存参数
     * @return null 表示成功，非 null 为错误信息
     */
    String saveForumNotice(int adminId, ForumNoticeSaveVO vo);

    /**
     * 分页查询全部评论（管理员，支持状态/内容/用户名/帖子标题筛选）
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param status     评论状态（可选）
     * @param content    内容关键词（可选）
     * @param author     用户名（可选）
     * @param topicTitle 帖子标题（可选）
     * @param tid        帖子ID（可选）
     * @return 分页结果
     */
    PageResult<AdminCommentVO> adminListComments(int page, int pageSize, String status, String content, String author, String topicTitle, Integer tid);

    /**
     * 删除评论（物理删除，并关闭相关待处理举报）
     *
     * @param id 评论ID
     * @return null 表示成功，非 null 为错误信息
     */
    String adminDeleteComment(int id);
}
