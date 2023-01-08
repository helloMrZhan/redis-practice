package com.zjq.commons.constant;

import lombok.Getter;

/**
 * 积分类型
 * @author zjq
 */
@Getter
public enum PointTypesConstant {

    sign(0),
    follow(1),
    feed(2),
    review(3)
    ;

    private int type;

    PointTypesConstant(int key) {
        this.type = key;
    }

}
