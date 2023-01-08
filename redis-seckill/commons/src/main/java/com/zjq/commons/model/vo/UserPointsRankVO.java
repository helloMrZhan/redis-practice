package com.zjq.commons.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户积分总排行榜
 * @author zjq
 */
@ApiModel(description = "用户积分总排行榜")
@Getter
@Setter
public class UserPointsRankVO extends ShortUserInfo {

    @ApiModelProperty("总积分")
    private int total;

    @ApiModelProperty("排名")
    private int ranks;

    @ApiModelProperty(value = "是否是自己", example = "0=否，1=是")
    private int isMe;

}
