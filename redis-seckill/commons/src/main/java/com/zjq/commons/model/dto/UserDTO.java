package com.zjq.commons.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 注册用户信息
 * @author zjq
 */
@Getter
@Setter
@ApiModel(description = "注册用户信息")
public class UserDTO implements Serializable {

    @ApiModelProperty("用户名")
    private String username;
    @ApiModelProperty("密码")
    private String password;
    @ApiModelProperty("手机号")
    private String phone;
    @ApiModelProperty("验证码")
    private String verifyCode;

}
