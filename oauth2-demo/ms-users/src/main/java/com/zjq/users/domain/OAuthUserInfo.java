package com.zjq.users.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 申请授权返回实体
 * @author zjq
 */
@Getter
@Setter
public class OAuthUserInfo implements Serializable {

    private String nickname;
    private String avatarUrl;
    private String accessToken;
    private String expireIn;
    private List<String> scopes;
    private String refreshToken;

}