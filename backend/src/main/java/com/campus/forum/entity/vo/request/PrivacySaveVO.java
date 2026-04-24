package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 保存隐私设置请求
 */
@Data
public class PrivacySaveVO {
    @Pattern(regexp = "(phone|email|qq|wx|gender)")
    String type;        // 隐私字段类型
    boolean status;     // 是否公开
}
