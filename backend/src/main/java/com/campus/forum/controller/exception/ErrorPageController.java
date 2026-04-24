package com.campus.forum.controller.exception;

import com.campus.forum.entity.RestBean;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * 全局错误页面控制器，统一处理 HTTP 错误响应
 */
@RestController
@RequestMapping({"${server.error.path:${error.path:/error}}"})
public class ErrorPageController extends AbstractErrorController {

    public ErrorPageController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    /**
     * 所有错误统一处理，自动解析状态码和原因
     * @param request 请求
     * @return 失败响应
     */
    @RequestMapping
    public RestBean<Void> error(HttpServletRequest request) {
        HttpStatus status = this.getStatus(request);
        Map<String, Object> errorAttributes = this.getErrorAttributes(request, this.getAttributeOptions());
        String message = this.convertErrorMessage(status)
                .orElse(errorAttributes.get("message").toString());
        return RestBean.failure(status.value(), message);
    }

    /**
     * 针对常见状态码转换为中文错误信息
     * @param status HTTP状态码
     * @return 错误信息
     */
    private Optional<String> convertErrorMessage(HttpStatus status){
        String value = switch (status.value()) {
            case 400 -> "请求参数有误";
            case 404 -> "请求的接口不存在";
            case 405 -> "请求方法错误";
            case 500 -> "内部错误，请联系管理员";
            default -> null;
        };
        return Optional.ofNullable(value);
    }

    /**
     * 错误属性获取选项，额外包含错误消息和异常类型
     * @return 选项
     */
    private ErrorAttributeOptions getAttributeOptions(){
        return ErrorAttributeOptions
                .defaults()
                .including(ErrorAttributeOptions.Include.MESSAGE,
                        ErrorAttributeOptions.Include.EXCEPTION);
    }
}
