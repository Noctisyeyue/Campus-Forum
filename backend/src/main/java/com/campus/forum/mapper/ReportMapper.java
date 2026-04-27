package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    @Select("select count(*) from db_report where uid = #{uid} and target_type = #{type} and target_id = #{targetId}")
    int userReportCount(@Param("uid") int uid, @Param("type") String type, @Param("targetId") int targetId);
}
