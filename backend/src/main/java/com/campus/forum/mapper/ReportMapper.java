package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 举报 Mapper
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * 查询用户是否已对指定目标发起过举报
     *
     * @param uid      用户ID
     * @param type     举报目标类型（topic/comment）
     * @param targetId 举报目标ID
     * @return 举报次数
     */
    @Select("select count(*) from db_report where uid = #{uid} and target_type = #{type} and target_id = #{targetId}")
    int userReportCount(@Param("uid") int uid, @Param("type") String type, @Param("targetId") int targetId);
}
