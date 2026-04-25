package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Interact;
import com.campus.forum.entity.dto.Topic;
import com.campus.forum.entity.dto.TopicType;
import com.campus.forum.entity.vo.request.AddCommentVO;
import com.campus.forum.entity.vo.request.TopicCreateVO;
import com.campus.forum.entity.vo.request.TopicUpdateVO;
import com.campus.forum.entity.vo.response.*;

import java.util.List;

/**
 * 帖子服务，包含发帖审核、列表查询、评论、互动等核心业务
 */
public interface TopicService extends IService<Topic> {
    List<TopicType> listTypes();
    String createTopic(int uid, TopicCreateVO vo);
    List<TopicPreviewVO> listTopicByPage(int page, int type);
    List<TopicTopVO> listTopTopics();
    TopicDetailVO getTopic(int tid, int uid);
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
    // 管理员方法：隐藏帖子
    void adminHideTopic(int tid);
    // 管理员方法：恢复已隐藏帖子
    void adminRestoreTopic(int tid);
    // 管理员方法：删除帖子（软删除）
    void adminDeleteTopic(int tid, int adminId);
    // 管理员方法：置顶帖子
    void adminTopTopic(int tid);
    // 管理员方法：取消置顶
    void adminUntopTopic(int tid);

    // 管理员方法：分页查询全部评论（支持帖子/用户筛选）
    List<AdminCommentVO> adminListComments(int page, Integer tid, Integer uid);
    // 管理员方法：删除评论（软删除）
    void adminDeleteComment(int id);
    // 管理员方法：恢复已删除评论
    void adminRestoreComment(int id);
}
