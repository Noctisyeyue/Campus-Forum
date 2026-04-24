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

    @Value("${spring.minio.endpoint}")
    String endpoint;        // MinIO 服务地址
    @Value("${spring.minio.username}")
    String username;        // 访问密钥
    @Value("${spring.minio.password}")
    String password;        // 密钥密码

    @Bean
    public MinioClient minioClient() {
        log.info("Init minio client...");
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(username, password)
                .build();
    }
}
