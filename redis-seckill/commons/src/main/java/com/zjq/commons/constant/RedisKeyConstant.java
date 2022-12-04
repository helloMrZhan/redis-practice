package com.zjq.commons.constant;

import lombok.Getter;

/**
 * Redis键枚举类
 * @author zjq
 */
@Getter
public enum RedisKeyConstant {

    verify_code("verify_code:", "验证码"),
    seckill_vouchers("seckill_vouchers:", "秒杀券的key"),
    lock_key("lockby:", "分布式锁的key"),
    following("following:", "关注集合Key"),
    followers("followers:", "粉丝集合key");

    private String key;
    private String desc;

    RedisKeyConstant(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

}
