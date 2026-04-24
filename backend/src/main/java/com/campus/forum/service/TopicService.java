package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Interact;
import com.campus.forum.entity.dto.Topic;
import com.campus.forum.entity.dto.TopicType;
import com.campus.forum.entity.vo.request.AddCommentVO;
import com.campus.forum.entity.vo.request.TopicCreateVO;
import com.campus.forum.entity.vo.request.TopicUpdateVO;
import com.campus.forum.entity.vo.response.CommentVO;
import com.campus.forum.entity.vo.response.TopicDetailVO;
import com.campus.forum.entity.vo.response.TopicPreviewVO;
import com.campus.forum.entity.vo.response.TopicTopVO;

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
}
