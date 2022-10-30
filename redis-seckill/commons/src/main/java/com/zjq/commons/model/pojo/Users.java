package com.zjq.commons.model.pojo;


import com.zjq.commons.model.base.BaseModel;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体类
 *
 * @Author zjq
 * @Date 2022/10/12
 */
@Getter
@Setter
public class Users extends BaseModel {

    // 主键
    private Integer id;
    // 用户名
    private String username;
    // 昵称
    private String nickname;
    // 密码
    private String password;
    // 手机号
    private String phone;
    // 邮箱
    private String email;
    // 头像
    private String avatarUrl;
    // 角色
    private String roles;

}
