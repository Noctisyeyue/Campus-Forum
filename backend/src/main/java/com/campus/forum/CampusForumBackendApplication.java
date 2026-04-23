package com.campus.forum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.forum.mapper")
public class CampusForumBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusForumBackendApplication.class, args);
    }
}
