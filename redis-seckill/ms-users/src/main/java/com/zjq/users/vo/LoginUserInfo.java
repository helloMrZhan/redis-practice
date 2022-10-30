package com.zjq.users.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 登录成功返回实体
 * @author zjq
 */
@Setter
@Getter
public class LoginUserInfo implements Serializable {

    private String nickname;
    private String token;
    private String avatarUrl;

}