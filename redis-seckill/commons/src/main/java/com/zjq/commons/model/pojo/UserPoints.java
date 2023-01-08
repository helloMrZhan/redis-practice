package com.zjq.commons.model.pojo;

import com.zjq.commons.model.base.BaseModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户积分实体
 * @author zjq
 */
@Getter
@Setter
public class UserPoints extends BaseModel {

    @ApiModelProperty("关联userId")
    private Integer fkUserId;
    @ApiModelProperty("积分")
    private Integer points;
    @ApiModelProperty(name = "类型",example = "0=签到，1=关注好友，2=添加Feed，3=添加商户评论")
    private Integer types;

}
