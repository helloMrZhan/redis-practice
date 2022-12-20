package com.zjq.follow.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.zjq.commons.constant.ApiConstant;
import com.zjq.commons.constant.RedisKeyConstant;
import com.zjq.commons.exception.ParameterException;
import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.pojo.Follow;
import com.zjq.commons.model.vo.ShortUserInfo;
import com.zjq.commons.model.vo.SignInUserInfo;
import com.zjq.commons.utils.AssertUtil;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.follow.mapper.FollowMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Value("${service.name.ms-feeds-server}")
    private String feedsServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 关注/取关
     *
     * @param followUserId 关注的用户ID
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
        SignInUserInfo userInfo = loadSignInuserInfo(accessToken);
        // 获取当前登录用户与需要关注用户的关注信息
        Follow follow = followMapper.selectFollow(userInfo.getId(), followUserId);

        // 如果没有关注信息，且要进行关注操作 -- 添加关注
        if (follow == null && isFollowed == 1) {
            // 添加关注信息
            int count = followMapper.save(userInfo.getId(), followUserId);
            // 添加关注列表到 Redis
            if (count == 1) {
                addToRedisSet(userInfo.getId(), followUserId);
                // 保存 Feed
                sendSaveOrRemoveFeed(followUserId, accessToken, 1);
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
                removeFromRedisSet(userInfo.getId(), followUserId);
                // 移除 Feed
                sendSaveOrRemoveFeed(followUserId, accessToken, 0);
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
                addToRedisSet(userInfo.getId(), followUserId);
                // 保存 Feed
                sendSaveOrRemoveFeed(followUserId, accessToken, 1);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,
                    "关注成功", path, "关注成功");
        }

        return ResultInfoUtil.buildSuccess(path, "操作成功");
    }

    /**
     * 添加关注列表到 Redis
     *
     * @param userId
     * @param followUserId
     */
    private void addToRedisSet(Integer userId, Integer followUserId) {
        redisTemplate.opsForSet().add(RedisKeyConstant.following.getKey() + userId, followUserId);
        redisTemplate.opsForSet().add(RedisKeyConstant.followers.getKey() + followUserId, userId);
    }

    /**
     * 移除 Redis 关注列表
     *
     * @param userId
     * @param followUserId
     */
    private void removeFromRedisSet(Integer userId, Integer followUserId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.following.getKey() + userId, followUserId);
        redisTemplate.opsForSet().remove(RedisKeyConstant.followers.getKey() + followUserId, userId);
    }

    /**
     * 获取登录用户信息
     *
     * @param accessToken
     * @return
     */
    private SignInUserInfo loadSignInuserInfo(String accessToken) {
        // 必须登录
        AssertUtil.mustLogin(accessToken);
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getMessage());
        }
        SignInUserInfo userInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInUserInfo(), false);
        return userInfo;
    }

    /**
     * 共同关注列表
     *
     * @param userId
     * @param accessToken
     * @param path
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultInfo findCommonsFriends(Integer userId, String accessToken, String path) {
        // 是否选择了查看对象
        AssertUtil.isTrue(userId == null || userId < 1,
                "请选择要查看的人");
        // 获取登录用户信息
        SignInUserInfo userInfo = loadSignInuserInfo(accessToken);
        // 获取登录用户的关注信息
        String loginuserKey = RedisKeyConstant.following.getKey() + userInfo.getId();
        // 获取登录用户查看对象的关注信息
        String userKey = RedisKeyConstant.following.getKey() + userId;
        // 计算交集
        Set<Integer> userIds = redisTemplate.opsForSet().intersect(loginuserKey, userKey);
        // 没有
        if (userIds == null || userIds.isEmpty()) {
            return ResultInfoUtil.buildSuccess(path, new ArrayList<ShortUserInfo>());
        }
        // 调用用户服务根据 ids 查询用户信息
        ResultInfo resultInfo = restTemplate.getForObject(usersServerName + "user/findByIds?access_token={accessToken}&ids={ids}",
                ResultInfo.class, accessToken, StrUtil.join(",", userIds));
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            resultInfo.setPath(path);
            return resultInfo;
        }
        // 处理结果集
        List<LinkedHashMap> dinnerInfoMaps = (ArrayList) resultInfo.getData();
        List<ShortUserInfo> userInfos = dinnerInfoMaps.stream()
                .map(user -> BeanUtil.fillBeanWithMap(user, new ShortUserInfo(), true))
                .collect(Collectors.toList());

        return ResultInfoUtil.buildSuccess(path, userInfos);
    }

    /**
     * 获取粉丝列表
     *
     * @param userId
     * @return
     */
    public Set<Integer> findFollowers(Integer userId) {
        AssertUtil.isNotNull(userId, "请选择要查看的用户");
        Set<Integer> followers = redisTemplate.opsForSet()
                .members(RedisKeyConstant.followers.getKey() + userId);
        return followers;
    }

    /**
     * 发送请求添加或者移除关注人的Feed列表
     *
     * @param followUserId 关注好友的ID
     * @param accessToken   当前登录用户token
     * @param type          0=取关 1=关注
     */
    private void sendSaveOrRemoveFeed(Integer followUserId, String accessToken, int type) {
        String feedsUpdateUrl = feedsServerName + "updateFollowingFeeds/"
                + followUserId + "?access_token=" + accessToken;
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 构建请求体（请求参数）
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("type", type);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(feedsUpdateUrl, entity, ResultInfo.class);
    }

}
