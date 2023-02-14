package com.zjq.commons.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 附近的人VO
 * @author zjq
 */
@ApiModel(description = "附近的人")
@Data
public class NearMeUserVO extends ShortUserInfo {

    @ApiModelProperty(value = "距离", example = "999m")
    private String distance;

}
