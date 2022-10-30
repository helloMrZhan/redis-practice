package com.zjq.commons.constant;

import lombok.Getter;

@Getter
public enum RedisKeyConstant {

    verify_code("verify_code:", "验证码");

    private String key;
    private String desc;

    RedisKeyConstant(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

}
