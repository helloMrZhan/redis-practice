package com.zjq.commons.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 *
 * 关注用户信息
 * @author zjq
 */
@Getter
@Setter
@ApiModel(description = "关注用户信息")
public class ShortUserInfo implements Serializable {

    @ApiModelProperty("主键")
    public Integer id;
    @ApiModelProperty("昵称")
    private String nickname;
    @ApiModelProperty("头像")
    private String avatarUrl;

}
