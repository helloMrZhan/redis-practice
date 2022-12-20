package com.zjq.feeds;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Feeds功能微服务
 * @author zjq
 */
@MapperScan("com.zjq.feeds.mapper")
@SpringBootApplication
public class FeedsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedsApplication.class);
    }

}
