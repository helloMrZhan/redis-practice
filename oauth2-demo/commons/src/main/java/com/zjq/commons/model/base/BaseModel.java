package com.zjq.commons.model.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体对象公共属性
 *
 * @Author zjq
 * @Date 2022/10/12
 */
@Getter
@Setter
public class BaseModel implements Serializable {

    private Integer id;
    private Date createDate;
    private Date updateDate;
    private int isValid;

}
