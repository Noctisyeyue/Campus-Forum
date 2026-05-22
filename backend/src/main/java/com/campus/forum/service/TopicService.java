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
    List<TopicType> listTypes();
    String createTopic(int uid, TopicCreateVO vo);
    List<TopicPreviewVO> listTopicByPage(int page, int type, String sort, String title);
    List<TopicPreviewVO> listActivityByPage(int page, String title);
    List<TopicPreviewVO> listNoticeTopicByPage(int page, String title);
    List<UserTopicVO> listUserTopics(int uid, int page, String status);
    List<TopicTopVO> listTopTopics();
    TopicDetailVO getTopic(int tid, int uid);
    TopicDetailVO getOwnTopic(int tid, int uid);
    TopicDetailVO adminGetTopic(int tid);
    ForumNoticeVO getForumNotice();
    void interact(Interact interact, boolean state);
    List<TopicPreviewVO> listTopicCollects(int uid);
    String updateTopic(int uid, TopicUpdateVO vo);
    String createComment(int uid, AddCommentVO vo);
    List<CommentVO> comments(int tid, int pageNumber);
    void deleteComment(int id, int uid);
    String deleteTopic(int uid, int tid);

    // 管理员方法：分页查询全部帖子（支持状态/分类/标题/作者筛选）
    List<AdminTopicVO> adminListTopics(int page, String status, Integer type, String title, String author);
    // 管理员方法：审核通过帖子
    void adminApproveTopic(int tid, int adminId);
    // 管理员方法：审核拒绝帖子
    void adminRejectTopic(int tid, int adminId, String reason);
    // 管理员方法：下架帖子（需填写原因）
    String adminHideTopic(int tid, String reason);
    // 管理员方法：上架帖子（恢复已下架帖子）
    String adminRestoreTopic(int tid);
    // 管理员方法：删除帖子（物理删除，不可逆）
    String adminDeleteTopic(int tid, int adminId);
    // 管理员方法：置顶帖子
    void adminTopTopic(int tid);
    // 管理员方法：取消置顶
    void adminUntopTopic(int tid);
    // 管理员方法：发布校园活动
    String publishActivity(int adminId, PublishActivityVO vo);
    // 管理员方法：发布教务通知
    String publishNoticeTopic(int adminId, PublishNoticeTopicVO vo);
    // 管理员方法：保存论坛公告
    String saveForumNotice(int adminId, ForumNoticeSaveVO vo);

    // 管理员方法：分页查询全部评论（支持状态/内容/用户名/帖子标题筛选）
    List<AdminCommentVO> adminListComments(int page, String status, String content, String author, String topicTitle);
    // 管理员方法：删除评论（软删除）
    void adminDeleteComment(int id);
}
