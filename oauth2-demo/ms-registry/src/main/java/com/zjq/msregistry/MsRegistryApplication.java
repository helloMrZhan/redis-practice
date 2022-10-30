package com.zjq.msregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;


/**
 * 注册中心
 * @author zjq
 */
//启动 eureka注册中心服务端相关组件
@EnableEurekaServer
@SpringBootApplication
public class MsRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsRegistryApplication.class, args);
    }

}
