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

    @Resource
    ImageService service;

    /**
     * 图片访问代理入口
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @GetMapping("/images/**")
    public void imageFetch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("Content-Type", "image/jpg");
        this.fetchImage(request, response);
    }

    /**
     * 从 MinIO 获取图片并写入响应流
     * @param request HTTP请求
     * @param response HTTP响应
     */
    private void fetchImage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String imagePath = request.getServletPath().substring(7);
        ServletOutputStream stream = response.getOutputStream();
        if(imagePath.length() <= 13) {
            response.setStatus(404);
            stream.println(RestBean.failure(404, "Not found").toString());
        } else {
            try {
                service.fetchImageFromMinio(stream, imagePath);
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
