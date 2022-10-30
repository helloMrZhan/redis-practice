package com.zjq.oauth2.server.service;

import com.zjq.commons.model.domain.SignInIdentity;
import com.zjq.commons.model.pojo.Users;
import com.zjq.commons.utils.AssertUtil;
import com.zjq.oauth2.server.mapper.UsersMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 登录校验
 * @author zjq
 */
@Service
public class UserService implements UserDetailsService {

    @Resource
    private UsersMapper usersMapper;

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

}
