package com.zjq.points;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * 积分微服务
 * @author zjq
 */
@MapperScan("com.zjq.points.mapper")
@SpringBootApplication
public class PointsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointsApplication.class, args);
    }

}
