package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.dto.StoreImage;
import com.campus.forum.mapper.AccountMapper;
import com.campus.forum.mapper.ImageStoreMapper;
import com.campus.forum.service.ImageService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.FlowUtils;
import io.minio.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 图片存储服务实现（MinIO）
 */
@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<ImageStoreMapper, StoreImage> implements ImageService {

    /** MinIO 客户端 */
    @Resource
    MinioClient client;

    /** 用户账号 Mapper */
    @Resource
    AccountMapper mapper;

    /** 限流工具 */
    @Resource
    FlowUtils flowUtils;

    /** 日期格式化器，用于按日期分目录存储图片 */
    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    /**
     * 从 MinIO 读取图片并写入输出流
     *
     * @param stream 输出流，图片数据写入此流
     * @param image  MinIO 中的图片对象路径
     */
    @Override
    public void fetchImageFromMinio(OutputStream stream, String image) throws Exception {
        // 构建"下载文件"的请求参数
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("campus-forum")
                .object(image)
                .build();
        // 执行下载
        GetObjectResponse response = client.getObject(args);
        // IOUtils.copy(输入流, 输出流)
        IOUtils.copy(response, stream);
    }

    /**
     * 上传图片到 MinIO（限流每小时 20 次），存储路径格式为 /cache/{yyyyMMdd}/{uuid}
     *
     * @param file 上传的图片文件
     * @param id   当前用户 ID
     * @return 上传成功返回图片路径，超频或失败返回 null
     */
    @Override
    public String uploadImage(MultipartFile file, int id) throws IOException {
        String key = Const.FORUM_IMAGE_COUNTER + id;
        if (!flowUtils.limitPeriodCounterCheck(key, 20, 3600))
            return null;
        String imageName = UUID.randomUUID().toString().replace("-", "");
        Date date = new Date();
        imageName = "/cache/" + format.format(date) + "/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("campus-forum")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        try {
            client.putObject(args);
            if (this.save(new StoreImage(id, imageName, date))) {
                return imageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("图片上传出现问题: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 上传头像到 MinIO，自动删除旧头像文件并更新数据库记录
     *
     * @param file 上传的头像文件
     * @param id   当前用户 ID
     * @return 上传成功返回新头像路径，失败返回 null
     */
    @Override
    public String uploadAvatar(MultipartFile file, int id) throws IOException {
        // 去掉“-”
        String imageName = UUID.randomUUID().toString().replace("-", "");
        imageName = "/avatar/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("campus-forum")                            // MinIO 中的桶名
                .stream(file.getInputStream(), file.getSize(), -1) // 文件内容、大小、未知长度用-1
                .object(imageName)                                 // 存储路径
                .build();
        try {
            client.putObject(args);                                // 1. 把文件上传到 MinIO
            String avatar = mapper.selectById(id).getAvatar();     // 2. 查数据库拿到旧头像路径
            this.deleteOldAvatar(avatar);                          // 3. 从 MinIO 删除旧头像文件
            if (mapper.update(null, Wrappers.<Account>update()
                    .eq("id", id).set("avatar", imageName)) > 0) { // 4. 把新头像路径写入数据库
                return imageName;                                  // 5. 返回新路径（成功）
            } else
                return null;                                       // 数据库更新失败
        } catch (Exception e) {
            log.error("图片上传出现问题: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除 MinIO 中的旧头像文件
     *
     * @param avatar 旧头像的对象路径，为空则跳过
     */
    private void deleteOldAvatar(String avatar) throws Exception {
        if (avatar == null || avatar.isEmpty()) return;
        RemoveObjectArgs remove = RemoveObjectArgs.builder()
                .bucket("campus-forum")
                .object(avatar)
                .build();
        client.removeObject(remove);
    }
}
