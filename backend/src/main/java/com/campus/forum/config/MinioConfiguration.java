package com.campus.forum.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 */
@Slf4j
@Configuration
public class MinioConfiguration {

    /** MinIO 服务地址 */
    @Value("${spring.minio.endpoint}")
    String endpoint;

    /** MinIO 访问密钥 */
    @Value("${spring.minio.username}")
    String username;

    /** MinIO 密钥密码 */
    @Value("${spring.minio.password}")
    String password;

    /**
     * 创建 MinIO 客户端并注册为 Spring Bean，供其他组件注入使用
     *
     * @return MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        log.info("Init minio client...");
        return MinioClient.builder()
                .endpoint(endpoint)               // MinIO 服务器地址，如 "http://localhost:9000"
                .credentials(username, password)  // 用户名和密码认证
                .build();                         // 构建出 MinioClient 实例
    }
}
