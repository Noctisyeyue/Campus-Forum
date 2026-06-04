package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.StoreImage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 图片存储服务
 */
public interface ImageService extends IService<StoreImage> {

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @param id   用户ID
     * @return 头像存储路径
     * @throws IOException 文件读取异常
     */
    String uploadAvatar(MultipartFile file, int id) throws IOException;

    /**
     * 上传普通图片（帖子/评论用）
     *
     * @param file 图片文件
     * @param id   用户ID
     * @return 图片存储路径
     * @throws IOException 文件读取异常
     */
    String uploadImage(MultipartFile file, int id) throws IOException;

    /**
     * 从 MinIO 获取图片并写入输出流
     *
     * @param stream 输出流
     * @param image  图片路径
     * @throws Exception MinIO 读取异常
     */
    void fetchImageFromMinio(OutputStream stream, String image) throws Exception;
}
