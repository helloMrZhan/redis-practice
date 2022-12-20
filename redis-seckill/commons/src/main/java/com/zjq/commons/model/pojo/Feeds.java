package com.zjq.commons.model.pojo;


import com.zjq.commons.model.base.BaseModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Feed信息类
 * @author zjq
 */
@Getter
@Setter
@ApiModel(description = "Feed信息类")
public class Feeds extends BaseModel {

    @ApiModelProperty("内容")
    private String content;
    @ApiModelProperty("用户id")
    private Integer fkUserId;
    @ApiModelProperty("点赞")
    private int praiseAmount;
    @ApiModelProperty("评论")
    private int commentAmount;
    @ApiModelProperty("关联的餐厅")
    private Integer fkRestaurantId;

}
