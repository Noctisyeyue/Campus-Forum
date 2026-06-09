package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.service.ImageService;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 静态资源代理控制器，通过路径 /images/** 从 MinIO 读取图片
 */
@Slf4j
@RestController
public class ObjectController {

    /** 图片存储服务 */
    @Resource
    ImageService service;

    /**
     * 图片访问代理入口，匹配所有 /images/** 路径
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应，直接写入图片二进制数据
     */
    @GetMapping("/images/**")
    public void imageFetch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("Content-Type", "image/jpg");
        this.fetchImage(request, response);
    }

    /**
     * 从 MinIO 获取图片并写入响应流，路径过短或图片不存在时返回 404
     *
     * @param request  HTTP 请求，用于提取图片路径
     * @param response HTTP 响应
     */
    private void fetchImage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 获取在 MinIO 中的存储路径
        String imagePath = request.getServletPath().substring(7);
        // 用来把图片二进制数据写给浏览器
        ServletOutputStream stream = response.getOutputStream();
        if(imagePath.length() <= 13) {
            response.setStatus(404);
                stream.println(RestBean.failure(404, "Not found").toString());
        } else {
            try {
                // 从 MinIO 读取图片数据写入 stream
                service.fetchImageFromMinio(stream, imagePath);
                // 告诉浏览器缓存 30 天（2592000 秒），下次访问同一张图不用再请求后端
                response.setHeader("Cache-Control", "max-age=2592000");
            } catch (ErrorResponseException e) {
                if(e.response().code() == 404) {
                    response.setStatus(404);
                    stream.println(RestBean.failure(404, "Not found").toString());
                } else {
                    log.error("从Minio获取图片出现异常: {}", e.getMessage(), e);
                }
            }
        }
    }
}
