package com.zjq.oauth2.server.mapper;

import com.zjq.commons.model.pojo.Users;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 * @author zjq
 */
public interface UsersMapper {

    /**
     *
     * 根据用户名 or 手机号 or 邮箱查询用户信息
     *
     * @param account
     * @return
     */
    @Select("select id, username, nickname, phone, email, " +
            "password, avatar_url, roles, is_valid from t_users where " +
            "(username = #{account} or phone = #{account} or email = #{account})")
    Users selectByAccountInfo(@Param("account") String account);

}
