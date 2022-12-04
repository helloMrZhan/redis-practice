package com.zjq.commons.model.pojo;

import com.zjq.commons.model.base.BaseModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户关注实体类
 * @author dell
 */
@ApiModel(description = "用户关注实体类")
@Getter
@Setter
public class Follow extends BaseModel {

    @ApiModelProperty("用户ID")
    private int userId;
    @ApiModelProperty("关注用户ID")
    private Integer followUserId;

}
