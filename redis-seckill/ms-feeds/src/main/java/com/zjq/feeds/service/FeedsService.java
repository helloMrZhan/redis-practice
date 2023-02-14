package com.zjq.feeds.service;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import com.zjq.commons.constant.ApiConstant;
import com.zjq.commons.constant.RedisKeyConstant;
import com.zjq.commons.exception.ParameterException;
import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.pojo.Feeds;
import com.zjq.commons.model.vo.FeedsVO;
import com.zjq.commons.model.vo.ShortUserInfo;
import com.zjq.commons.model.vo.SignInUserInfo;
import com.zjq.commons.utils.AssertUtil;
import com.zjq.feeds.mapper.FeedsMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feed服务类
 * @author zjq
 */
@Service
public class FeedsService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-follow-server}")
    private String followServerName;
    @Value("${service.name.ms-users-server}")
    private String usersServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private FeedsMapper feedsMapper;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 根据时间由近至远，每次查询 6 条 Feed
     *
     * @param page
     * @param accessToken
     * @return
     */
    public List<FeedsVO> selectForPage(Integer page, String accessToken) {
        if (page == null) {
            page = 1;
        }
        // 获取登录用户
        SignInUserInfo userInfo = loadSignInUserInfo(accessToken);
        // 我关注的好友的 Feedkey
        String key = RedisKeyConstant.following_feeds.getKey() + userInfo.getId();
        // SortedSet 的 ZREVRANGE 命令是闭区间
        long start = (page - 1) * ApiConstant.PAGE_SIZE;
        long end = page * ApiConstant.PAGE_SIZE - 1;
        Set<Integer> feedIds = redisTemplate.opsForZSet().reverseRange(key, start, end);
        if (feedIds == null || feedIds.isEmpty()) {
            return Lists.newArrayList();
        }
        // 根据多主键查询 Feed
        List<Feeds> feeds = feedsMapper.findFeedsByIds(feedIds);
        // 初始化关注好友 ID 集合
        List<Integer> followinguserIds = new ArrayList<>();
        // 添加用户 ID 至集合，顺带将 Feeds 转为 Vo 对象
        List<FeedsVO> feedsVOS = feeds.stream().map(feed -> {
            FeedsVO feedsVO = new FeedsVO();
            BeanUtil.copyProperties(feed, feedsVO);
            // 添加用户 ID
            followinguserIds.add(feed.getFkUserId());
            return feedsVO;
        }).collect(Collectors.toList());
        // 远程调用获取 Feed 中用户信息
        ResultInfo resultInfo = restTemplate.getForObject(usersServerName + "user/findByIds?access_token=${accessToken}&ids={ids}",
                ResultInfo.class, accessToken, followinguserIds);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        List<LinkedHashMap> userInfoMaps = (ArrayList) resultInfo.getData();
        // 构建一个 key 为用户 ID，value 为 ShortuserInfo 的 Map
        Map<Integer, ShortUserInfo> userInfos = userInfoMaps.stream()
                .collect(Collectors.toMap(
                        // key
                        user -> (Integer) user.get("id"),
                        // value
                        user -> BeanUtil.fillBeanWithMap(user, new ShortUserInfo(), true)
                ));
        // 循环 VO 集合，根据用户 ID 从 Map 中获取用户信息并设置至 VO 对象
        feedsVOS.forEach(feedsVO -> {
            feedsVO.setUserInfo(userInfos.get(feedsVO.getFkUserId()));
        });
        return feedsVOS;
    }

    /**
     * 变更 Feed
     *
     * @param followinguserId 关注的好友 ID
     * @param accessToken      登录用户token
     * @param type             1 关注 0 取关
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFollowingFeed(Integer followinguserId, String accessToken, int type) {
        // 请选择关注的好友
        AssertUtil.isTrue(followinguserId == null || followinguserId < 1,
                "请选择关注的好友");
        // 获取登录用户信息
        SignInUserInfo userInfo = loadSignInUserInfo(accessToken);
        // 获取关注/取关的用户的所有 Feed
        List<Feeds> feedsList = feedsMapper.findByUserId(followinguserId);
        String key = RedisKeyConstant.following_feeds.getKey() + userInfo.getId();
        if (type == 0) {
            // 取关
            List<Integer> feedIds = feedsList.stream()
                    .map(feed -> feed.getId())
                    .collect(Collectors.toList());
            redisTemplate.opsForZSet().remove(key, feedIds.toArray(new Integer[]{}));
        } else {
            // 关注
            Set<ZSetOperations.TypedTuple> typedTuples =
                    feedsList.stream()
                            .map(feed -> new DefaultTypedTuple<>(feed.getId(), (double) feed.getUpdateDate().getTime()))
                            .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(key, typedTuples);
        }
    }

    /**
     * 删除 Feed
     *
     * @param id
     * @param accessToken
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id, String accessToken) {
        // 请选择要删除的 Feed
        AssertUtil.isTrue(id == null || id < 1, "请选择要删除的Feed");
        // 获取登录用户
        SignInUserInfo userInfo = loadSignInUserInfo(accessToken);
        // 获取 Feed 内容
        Feeds feeds = feedsMapper.findById(id);
        // 判断 Feed 是否已经被删除且只能删除自己的 Feed
        AssertUtil.isTrue(feeds == null, "该Feed已被删除");
        AssertUtil.isTrue(!feeds.getFkUserId().equals(userInfo.getId()),
                "只能删除自己的Feed");
        // 删除
        int count = feedsMapper.delete(id);
        if (count == 0) {
            return;
        }
        // 将内容从粉丝的集合中删除 -- 异步消息队列优化
        // 先获取我的粉丝
        List<Integer> followers = findFollowers(userInfo.getId());
        // 移除 Feed
        followers.forEach(follower -> {
            String key = RedisKeyConstant.following_feeds.getKey() + follower;
            redisTemplate.opsForZSet().remove(key, feeds.getId());
        });
    }

    /**
     * 添加 Feed
     *
     * @param feeds
     * @param accessToken
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Feeds feeds, String accessToken) {
        // 校验 Feed 内容不能为空，不能太长
        AssertUtil.isNotEmpty(feeds.getContent(), "请输入内容");
        AssertUtil.isTrue(feeds.getContent().length() > 255, "输入内容太多，请重新输入");
        // 获取登录用户信息
        SignInUserInfo userInfo = loadSignInUserInfo(accessToken);
        // Feed 关联用户信息
        feeds.setFkUserId(userInfo.getId());
        // 添加 Feed
        int count = feedsMapper.save(feeds);
        AssertUtil.isTrue(count == 0, "添加失败");
        // 推送到粉丝的列表中 -- 后续这里应该采用异步消息队列解决性能问题
        // 先获取粉丝 id 集合
        List<Integer> followers = findFollowers(userInfo.getId());
        // 推送 Feed
        long now = System.currentTimeMillis();
        followers.forEach(follower -> {
            String key = RedisKeyConstant.following_feeds.getKey() + follower;
            redisTemplate.opsForZSet().add(key, feeds.getId(), now);
        });
    }

    /**
     * 获取粉丝 id 集合
     *
     * @param userId
     * @return
     */
    private List<Integer> findFollowers(Integer userId) {
        String url = followServerName + "followers/" + userId;
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        List<Integer> followers = (List<Integer>) resultInfo.getData();
        return followers;
    }

    /**
     * 获取登录用户信息
     *
     * @param accessToken
     * @return
     */
    private SignInUserInfo loadSignInUserInfo(String accessToken) {
        // 必须登录
        AssertUtil.mustLogin(accessToken);
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        SignInUserInfo userInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInUserInfo(), false);
        if (userInfo == null) {
            throw new ParameterException(ApiConstant.NO_LOGIN_CODE, ApiConstant.NO_LOGIN_MESSAGE);
        }
        return userInfo;
    }

}
