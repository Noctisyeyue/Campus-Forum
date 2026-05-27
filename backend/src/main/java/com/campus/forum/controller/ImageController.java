package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.service.ImageService;
import com.campus.forum.utils.Const;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 图片上传控制器，处理普通图片和头像上传
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Resource
    ImageService service;

    /**
     * 上传普通图片（限5MB，每小时限20次）
     * @param file 图片文件
     * @param id 当前用户ID
     * @param response HTTP响应
     * @return 图片访问路径
     */
    @PostMapping("/cache")
    public RestBean<String> uploadImage(@RequestParam("file") MultipartFile file,
                                        @RequestAttribute(Const.ATTR_USER_ID) int id,
                                        HttpServletResponse response) throws IOException {
        if(file.getSize() > 1024 * 1024 * 5)
            return RestBean.failure(400, "图片不能大于5MB");
        log.info("正在进行图片上传操作...");
        String url = service.uploadImage(file, id);
        if(url != null) {
            log.info("图片上传成功，大小: {}", file.getSize());
            return RestBean.success(url);
        } else {
            response.setStatus(400);
            return RestBean.failure(400, "图片上传失败，请联系管理员！");
        }
    }

    /**
     * 上传头像（限2MB）
     * @param file 头像文件
     * @param id 当前用户ID
     * @return 头像访问路径
     */
    @PostMapping("/avatar")
    public RestBean<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                         @RequestAttribute(Const.ATTR_USER_ID) int id) throws IOException {
        if(file.getSize() > 1024 * 1024 * 2)
            return RestBean.failure(400, "头像图片不能大于2MB");
        log.info("正在进行头像上传操作...");
        String url = service.uploadAvatar(file, id);
        if(url != null) {
            log.info("头像上传成功，大小: {}", file.getSize());
            return RestBean.success(url);
        } else {
            return RestBean.failure(400, "头像上传失败，请联系管理员！");
        }
    }
}
