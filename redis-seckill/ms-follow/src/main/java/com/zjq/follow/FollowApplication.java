package com.zjq.follow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 好友功能微服务
 * @author zjq
 */
@MapperScan("com.zjq.follow.mapper")
@SpringBootApplication
public class FollowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FollowApplication.class);
    }

}
