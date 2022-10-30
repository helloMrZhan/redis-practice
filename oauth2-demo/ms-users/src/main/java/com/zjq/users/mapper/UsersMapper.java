package com.zjq.users.mapper;

import com.zjq.commons.model.dto.UserDTO;
import com.zjq.commons.model.pojo.Users;
import org.apache.ibatis.annotations.Insert;
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


    /**
     * 根据手机号查询用户信息
     * @param phone
     * @return
     */
    @Select("select id, username, phone, email, is_valid " +
            " from t_users where phone = #{phone}")
    Users selectByPhone(@Param("phone") String phone);


    /**
     * 根据用户名查询用户信息
     * @param username
     * @return
     */
    @Select("select id, username, phone, email, is_valid " +
            " from t_users where username = #{username}")
    Users selectByUsername(@Param("username") String username);


    /**
     * 新增用户信息
     * @param userDTO
     * @return
     */
    @Insert("insert into " +
            " t_users (username, password, phone, roles, is_valid, create_date, update_date) " +
            " values (#{username}, #{password}, #{phone}, \"ROLE_USER\", 1, now(), now())")
    int saveUser(UserDTO userDTO);

}
