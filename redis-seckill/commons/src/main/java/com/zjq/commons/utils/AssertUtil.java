package com.zjq.commons.utils;

import cn.hutool.core.util.StrUtil;
import com.zjq.commons.constant.ApiConstant;
import com.zjq.commons.exception.ParameterException;

/**
 * 断言工具类
 *
 * @Author zjq
 * @Date 2022/10/12
 */
public class AssertUtil {

    /**
     * 判断字符串非空
     *
     * @param str
     * @param message
     */
    public static void isNotEmpty(String str, String... message) {
        if (StrUtil.isBlank(str)) {
            execute(message);
        }
    }

    /**
     * 判断对象非空
     *
     * @param obj
     * @param message
     */
    public static void isNotNull(Object obj, String... message) {
        if (obj == null) {
            execute(message);
        }
    }

    /**
     * 判断结果是否为真
     *
     * @param isTrue
     * @param message
     */
    public static void isTrue(boolean isTrue, String... message) {
        if (isTrue) {
            execute(message);
        }
    }

    /**
     * 最终执行方法
     *
     * @param message
     */
    private static void execute(String... message) {
        String msg = ApiConstant.ERROR_MESSAGE;
        if (message != null && message.length > 0) {
            msg = message[0];
        }
        throw new ParameterException(msg);
    }

}
