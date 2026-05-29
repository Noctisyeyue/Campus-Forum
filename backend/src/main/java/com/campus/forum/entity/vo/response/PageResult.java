package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果包装，包含数据列表和总记录数
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> list;

    /** 总记录数 */
    private long total;

    public PageResult(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }
}
