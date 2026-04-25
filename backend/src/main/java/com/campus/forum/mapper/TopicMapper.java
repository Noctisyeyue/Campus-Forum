package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.Interact;
import com.campus.forum.entity.dto.Topic;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 帖子 Mapper，包含互动（点赞/收藏）的自定义 SQL
 */
@Mapper
public interface TopicMapper extends BaseMapper<Topic> {

    /**
     * 批量插入点赞记录
     */
    @Insert("""
            <script>
                insert ignore into db_topic_interact_like values
                <foreach collection="interacts" item="item" separator=",">
                    (#{item.tid}, #{item.uid}, #{item.time})
                </foreach>
            </script>
            """)
    void addLikeInteract(@Param("interacts") List<Interact> interacts);

    /**
     * 批量插入收藏记录
     */
    @Insert("""
            <script>
                insert ignore into db_topic_interact_collect values
                <foreach collection="interacts" item="item" separator=",">
                    (#{item.tid}, #{item.uid}, #{item.time})
                </foreach>
            </script>
            """)
    void addCollectInteract(@Param("interacts") List<Interact> interacts);

    /**
     * 批量插入互动记录
     *
     * @param interacts 互动记录列表
     * @param type 互动类型，只允许 like 或 collect
     */
    default void addInteract(List<Interact> interacts, String type) {
        switch (type) {
            case "like" -> this.addLikeInteract(interacts);
            case "collect" -> this.addCollectInteract(interacts);
            default -> throw new IllegalArgumentException("不支持的互动类型: " + type);
        }
    }

    /**
     * 批量删除点赞记录
     */
    @Delete("""
            <script>
                delete from db_topic_interact_like where
                <foreach collection="interacts" item="item" separator=" or ">
                    (tid = #{item.tid} and uid = #{item.uid})
                </foreach>
            </script>
            """)
    int deleteLikeInteract(@Param("interacts") List<Interact> interacts);

    /**
     * 批量删除收藏记录
     */
    @Delete("""
            <script>
                delete from db_topic_interact_collect where
                <foreach collection="interacts" item="item" separator=" or ">
                    (tid = #{item.tid} and uid = #{item.uid})
                </foreach>
            </script>
            """)
    int deleteCollectInteract(@Param("interacts") List<Interact> interacts);

    /**
     * 删除帖子全部点赞记录
     */
    @Delete("delete from db_topic_interact_like where tid = #{tid}")
    int deleteLikeByTid(@Param("tid") int tid);

    /**
     * 删除帖子全部收藏记录
     */
    @Delete("delete from db_topic_interact_collect where tid = #{tid}")
    int deleteCollectByTid(@Param("tid") int tid);

    /**
     * 批量删除互动记录
     *
     * @param interacts 互动记录列表
     * @param type 互动类型，只允许 like 或 collect
     * @return 实际删除条数
     */
    default int deleteInteract(List<Interact> interacts, String type) {
        return switch (type) {
            case "like" -> this.deleteLikeInteract(interacts);
            case "collect" -> this.deleteCollectInteract(interacts);
            default -> throw new IllegalArgumentException("不支持的互动类型: " + type);
        };
    }

    /**
     * 统计帖子点赞数量
     */
    @Select("select count(*) from db_topic_interact_like where tid = #{tid}")
    int likeCount(@Param("tid") int tid);

    /**
     * 统计帖子收藏数量
     */
    @Select("select count(*) from db_topic_interact_collect where tid = #{tid}")
    int collectCount(@Param("tid") int tid);

    /**
     * 统计帖子的互动数量
     *
     * @param tid 帖子ID
     * @param type 互动类型，只允许 like 或 collect
     * @return 互动数量
     */
    default int interactCount(int tid, String type) {
        return switch (type) {
            case "like" -> this.likeCount(tid);
            case "collect" -> this.collectCount(tid);
            default -> throw new IllegalArgumentException("不支持的互动类型: " + type);
        };
    }

    /**
     * 查询用户对帖子是否点过赞
     */
    @Select("select count(*) from db_topic_interact_like where tid = #{tid} and uid = #{uid}")
    int userLikeCount(@Param("tid") int tid, @Param("uid") int uid);

    /**
     * 查询用户对帖子是否收藏过
     */
    @Select("select count(*) from db_topic_interact_collect where tid = #{tid} and uid = #{uid}")
    int userCollectCount(@Param("tid") int tid, @Param("uid") int uid);

    /**
     * 查询用户是否对帖子有指定互动
     *
     * @param tid 帖子ID
     * @param uid 用户ID
     * @param type 互动类型，只允许 like 或 collect
     * @return 互动数量
     */
    default int userInteractCount(int tid, int uid, String type) {
        return switch (type) {
            case "like" -> this.userLikeCount(tid, uid);
            case "collect" -> this.userCollectCount(tid, uid);
            default -> throw new IllegalArgumentException("不支持的互动类型: " + type);
        };
    }

    /**
     * 查询用户收藏的帖子列表
     */
    @Select("""
            select t.* from db_topic_interact_collect c
            inner join db_topic t on c.tid = t.id
            where c.uid = #{uid} and t.status = 'published'
            order by c.time desc
            """)
    List<Topic> collectTopics(@Param("uid") int uid);
}
