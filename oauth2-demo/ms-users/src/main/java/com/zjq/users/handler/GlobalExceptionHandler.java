package com.zjq.users.handler;


import com.zjq.commons.exception.ParameterException;
import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.utils.ResultInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 全局异常处理类
 * @author zjq
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Resource
    private HttpServletRequest request;

    @ExceptionHandler(ParameterException.class)
    public ResultInfo<Map<String, String>> handlerParameterException(ParameterException ex) {
        String path = request.getRequestURI();
        ResultInfo<Map<String, String>> resultInfo =
                ResultInfoUtil.buildError(ex.getErrorCode(), ex.getMessage(), path);
        return resultInfo;
    }

    @ExceptionHandler(Exception.class)
    public ResultInfo<Map<String, String>> handlerException(Exception ex) {
        log.info("未知异常：{}", ex);
        String path = request.getRequestURI();
        ResultInfo<Map<String, String>> resultInfo =
                ResultInfoUtil.buildError(path);
        return resultInfo;
    }

}
