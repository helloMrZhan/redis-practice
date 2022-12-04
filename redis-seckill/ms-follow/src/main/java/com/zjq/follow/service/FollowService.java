package com.zjq.follow.service;

import cn.hutool.core.bean.BeanUtil;
import com.zjq.commons.constant.ApiConstant;
import com.zjq.commons.constant.RedisKeyConstant;
import com.zjq.commons.exception.ParameterException;
import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.pojo.Follow;
import com.zjq.commons.model.vo.SignInUserInfo;
import com.zjq.commons.utils.AssertUtil;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.follow.mapper.FollowMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

/**
 * 关注/取关业务逻辑层
 * @author zjq
 */
@Service
public class FollowService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-users-server}")
    private String usersServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 关注/取关
     *
     * @param followUserId 关注的食客ID
     * @param isFollowed    是否关注 1=关注 0=取关
     * @param accessToken   登录用户token
     * @param path          访问地址
     * @return
     */
    public ResultInfo follow(Integer followUserId, int isFollowed,
                             String accessToken, String path) {
        // 是否选择了关注对象
        AssertUtil.isTrue(followUserId == null || followUserId < 1,
                "请选择要关注的人");
        // 获取登录用户信息 (封装方法)
        SignInUserInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取当前登录用户与需要关注用户的关注信息
        Follow follow = followMapper.selectFollow(dinerInfo.getId(), followUserId);

        // 如果没有关注信息，且要进行关注操作 -- 添加关注
        if (follow == null && isFollowed == 1) {
            // 添加关注信息
            int count = followMapper.save(dinerInfo.getId(), followUserId);
            // 添加关注列表到 Redis
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followUserId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,
                    "关注成功", path, "关注成功");
        }

        // 如果有关注信息，且目前处于关注状态，且要进行取关操作 -- 取关关注
        if (follow != null && follow.getIsValid() == 1 && isFollowed == 0) {
            // 取关
            int count = followMapper.update(follow.getId(), isFollowed);
            // 移除 Redis 关注列表
            if (count == 1) {
                removeFromRedisSet(dinerInfo.getId(), followUserId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,
                    "成功取关", path, "成功取关");
        }

        // 如果有关注信息，且目前处于取关状态，且要进行关注操作 -- 重新关注
        if (follow != null && follow.getIsValid() == 0 && isFollowed == 1) {
            // 重新关注
            int count = followMapper.update(follow.getId(), isFollowed);
            // 添加关注列表到 Redis
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followUserId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,
                    "关注成功", path, "关注成功");
        }

        return ResultInfoUtil.buildSuccess(path, "操作成功");
    }

    /**
     * 添加关注列表到 Redis
     *
     * @param dinerId
     * @param followUserId
     */
    private void addToRedisSet(Integer dinerId, Integer followUserId) {
        redisTemplate.opsForSet().add(RedisKeyConstant.following.getKey() + dinerId, followUserId);
        redisTemplate.opsForSet().add(RedisKeyConstant.followers.getKey() + followUserId, dinerId);
    }

    /**
     * 移除 Redis 关注列表
     *
     * @param dinerId
     * @param followUserId
     */
    private void removeFromRedisSet(Integer dinerId, Integer followUserId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.following.getKey() + dinerId, followUserId);
        redisTemplate.opsForSet().remove(RedisKeyConstant.followers.getKey() + followUserId, dinerId);
    }

    /**
     * 获取登录用户信息
     *
     * @param accessToken
     * @return
     */
    private SignInUserInfo loadSignInDinerInfo(String accessToken) {
        // 必须登录
        AssertUtil.mustLogin(accessToken);
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getMessage());
        }
        SignInUserInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInUserInfo(), false);
        return dinerInfo;
    }

}
