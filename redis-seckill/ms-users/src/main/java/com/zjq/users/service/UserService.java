package com.zjq.users.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.zjq.commons.constant.ApiConstant;
import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.domain.SignInIdentity;
import com.zjq.commons.model.dto.UserDTO;
import com.zjq.commons.model.pojo.Users;
import com.zjq.commons.utils.AssertUtil;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.oauth2.server.mapper.UsersMapper;
import com.zjq.users.config.OAuth2ClientConfiguration;
import com.zjq.users.domain.OAuthUserInfo;
import com.zjq.users.vo.LoginUserInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

/**
 * 登录校验
 * @author zjq
 */
@Service
public class UserService implements UserDetailsService {

    @Resource
    private UsersMapper usersMapper;
    @Resource
    private RestTemplate restTemplate;
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private OAuth2ClientConfiguration oAuth2ClientConfiguration;

    @Resource
    private SendVerifyCodeService sendVerifyCodeService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AssertUtil.isNotEmpty(username, "请输入用户名");
        Users users = usersMapper.selectByAccountInfo(username);
        if (users == null) {
            throw new UsernameNotFoundException("用户名或密码错误，请重新输入");
        }
        // 初始化登录认证对象
        SignInIdentity signInIdentity = new SignInIdentity();
        // 拷贝属性
        BeanUtils.copyProperties(users, signInIdentity);
        return signInIdentity;
    }

    /**
     * 登录
     *
     * @param account  帐号：用户名或手机或邮箱
     * @param password 密码
     * @param path     请求路径
     * @return
     */
    public ResultInfo signIn(String account, String password, String path) {
        // 参数校验
        AssertUtil.isNotEmpty(account, "请输入登录帐号");
        AssertUtil.isNotEmpty(password, "请输入登录密码");
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 构建请求体（请求参数）
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("username", account);
        body.add("password", password);
        body.setAll(BeanUtil.beanToMap(oAuth2ClientConfiguration));
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        // 设置 Authorization
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(oAuth2ClientConfiguration.getClientId(),
                oAuth2ClientConfiguration.getSecret()));
        // 发送请求
        ResponseEntity<ResultInfo> result = restTemplate.postForEntity(oauthServerName + "oauth/token", entity, ResultInfo.class);
        // 处理返回结果
        AssertUtil.isTrue(result.getStatusCode() != HttpStatus.OK, "登录失败");
        ResultInfo resultInfo = result.getBody();
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            // 登录失败
            resultInfo.setData(resultInfo.getMessage());
            return resultInfo;
        }
        // 这里的 Data 是一个 LinkedHashMap 转成了域对象 OAuthDinerInfo
        OAuthUserInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new OAuthUserInfo(), false);
        // 根据业务需求返回视图对象
        LoginUserInfo loginDinerInfo = new LoginUserInfo();
        loginDinerInfo.setToken(dinerInfo.getAccessToken());
        loginDinerInfo.setAvatarUrl(dinerInfo.getAvatarUrl());
        loginDinerInfo.setNickname(dinerInfo.getNickname());
        return ResultInfoUtil.buildSuccess(path, loginDinerInfo);
    }


    /**
     * 用户注册
     *
     * @param userDTO
     * @param path
     * @return
     */
    public ResultInfo register(UserDTO userDTO, String path) {
        // 参数非空校验
        String username = userDTO.getUsername();
        AssertUtil.isNotEmpty(username, "请输入用户名");
        String password = userDTO.getPassword();
        AssertUtil.isNotEmpty(password, "请输入密码");
        String phone = userDTO.getPhone();
        AssertUtil.isNotEmpty(phone, "请输入手机号");
        String verifyCode = userDTO.getVerifyCode();
        AssertUtil.isNotEmpty(verifyCode, "请输入验证码");
        // 获取验证码
        String code = sendVerifyCodeService.getCodeByPhone(phone);
        // 验证是否过期
        AssertUtil.isNotEmpty(code, "验证码已过期，请重新发送");
        // 验证码一致性校验
        AssertUtil.isTrue(!userDTO.getVerifyCode().equals(code), "验证码不一致，请重新输入");
        // 验证用户名是否已注册
        Users users = usersMapper.selectByUsername(username.trim());
        AssertUtil.isTrue(users != null, "用户名已存在，请重新输入");
        // 注册
        // 密码加密
        userDTO.setPassword(DigestUtil.md5Hex(password.trim()));
        usersMapper.saveUser(userDTO);
        // 自动登录
        return signIn(username.trim(), password.trim(), path);
    }

    /**
     * 校验手机号是否已注册
     */
    public void checkPhoneIsRegistered(String phone) {
        AssertUtil.isNotEmpty(phone, "手机号不能为空");
        Users users = usersMapper.selectByPhone(phone);
        AssertUtil.isTrue(users == null, "该手机号未注册");
        AssertUtil.isTrue(users.getIsValid() == 0, "该用户已锁定，请先解锁");
    }

}
