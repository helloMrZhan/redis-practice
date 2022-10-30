package com.zjq.users.controller;

import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.domain.SignInIdentity;
import com.zjq.commons.model.dto.UserDTO;
import com.zjq.commons.model.vo.SignInDinerInfo;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.users.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户中心
 */
@RestController
@RequestMapping("user")
public class UserController {

    @Resource
    private HttpServletRequest request;

    @Resource
//    private RedisTokenStore redisTokenStore;

    @Autowired
    private UserService userService;


    /**
     * 登录
     *
     * @param account
     * @param password
     * @return
     */
    @GetMapping("signin")
    public ResultInfo signIn(String account, String password) {
        return userService.signIn(account, password, request.getServletPath());
    }

    /**
     * 校验手机号是否已注册
     *
     * @param phone
     * @return
     */
    @GetMapping("checkPhone")
    public ResultInfo checkPhone(String phone) {
        userService.checkPhoneIsRegistered(phone);
        return ResultInfoUtil.buildSuccess(request.getServletPath());
    }

    /**
     * 注册
     *
     * @param userDTO
     * @return
     */
    @PostMapping("register")
    public ResultInfo register(@RequestBody UserDTO userDTO) {
        return userService.register(userDTO, request.getServletPath());
    }

}
