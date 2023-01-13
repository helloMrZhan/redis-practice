package com.zjq.points.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.zjq.commons.constant.ApiConstant;
import com.zjq.commons.constant.RedisKeyConstant;
import com.zjq.commons.exception.ParameterException;
import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.pojo.UserPoints;
import com.zjq.commons.model.vo.UserPointsRankVO;
import com.zjq.commons.model.vo.ShortUserInfo;
import com.zjq.commons.model.vo.SignInUserInfo;
import com.zjq.commons.utils.AssertUtil;
import com.zjq.points.mapper.UserPointsMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 积分业务逻辑层
 * @author zjq
 */
@Service
public class UserPointsService {

    @Resource
    private UserPointsMapper userPointsMapper;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisTemplate redisTemplate;
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-users-server}")
    private String usersServerName;
    // 排行榜 TOPN
    private static final int TOPN = 20;

    /**
     * 添加积分
     *
     * @param userId 用户ID
     * @param points  积分
     * @param types   类型 0=签到，1=关注好友，2=添加Feed，3=添加商户评论
     */
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Integer userId, Integer points, Integer types) {
        // 基本参数校验
        AssertUtil.isTrue(userId == null || userId < 1, "用户不能为空");
        AssertUtil.isTrue(points == null || points < 1, "积分不能为空");
        AssertUtil.isTrue(types == null, "请选择对应的积分类型");

        // 插入数据库
        UserPoints userPoints = new UserPoints();
        userPoints.setFkUserId(userId);
        userPoints.setPoints(points);
        userPoints.setTypes(types);
        userPointsMapper.save(userPoints);

        // 将积分保存到 Redis
        redisTemplate.opsForZSet().incrementScore(
                RedisKeyConstant.user_points.getKey(), userId, points);
    }

    /**
     * 查询前 20 积分排行榜，并显示个人排名 -- Redis
     *
     * @param accessToken
     * @return
     */
    public List<UserPointsRankVO> findUserPointRankFromRedis(String accessToken) {
        // 获取登录用户信息
        SignInUserInfo signInUserInfo = loadSignInUserInfo(accessToken);
        // 统计积分排行榜
        Set<ZSetOperations.TypedTuple<Integer>> rangeWithScores = redisTemplate.opsForZSet().reverseRangeWithScores(
                RedisKeyConstant.user_points.getKey(), 0, 19);
        if (rangeWithScores == null || rangeWithScores.isEmpty()) {
            return Lists.newArrayList();
        }
        // 初始化用户 ID 集合
        List<Integer> rankuserIds = Lists.newArrayList();
        // 根据 key：用户 ID value：积分信息 构建一个 Map
        Map<Integer, UserPointsRankVO> ranksMap = new LinkedHashMap<>();
        // 初始化排名
        int rank = 1;
        // 循环处理排行榜，添加排名信息
        for (ZSetOperations.TypedTuple<Integer> rangeWithScore : rangeWithScores) {
            // 用户ID
            Integer userId = rangeWithScore.getValue();
            // 积分
            int points = rangeWithScore.getScore().intValue();
            // 将用户 ID 添加至用户 ID 集合
            rankuserIds.add(userId);
            UserPointsRankVO userPointsRankVO = new UserPointsRankVO();
            userPointsRankVO.setId(userId);
            userPointsRankVO.setRanks(rank);
            userPointsRankVO.setTotal(points);
            // 将 VO 对象添加至 Map 中
            ranksMap.put(userId, userPointsRankVO);
            // 排名 +1
            rank++;
        }

        // 获取 users 用户信息
        ResultInfo resultInfo = restTemplate.getForObject(usersServerName +
                        "user/findByIds?access_token=${accessToken}&ids={ids}",
                ResultInfo.class, accessToken, StrUtil.join(",", rankuserIds));
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        List<LinkedHashMap> dinerInfoMaps = (List<LinkedHashMap>) resultInfo.getData();
        // 完善用户昵称和头像
        for (LinkedHashMap dinerInfoMap : dinerInfoMaps) {
            ShortUserInfo shortDinerInfo = BeanUtil.fillBeanWithMap(dinerInfoMap,
                    new ShortUserInfo(), false);
            UserPointsRankVO rankVO = ranksMap.get(shortDinerInfo.getId());
            rankVO.setNickname(shortDinerInfo.getNickname());
            rankVO.setAvatarUrl(shortDinerInfo.getAvatarUrl());
        }

        // 判断个人是否在 ranks 中，如果在，添加标记直接返回
        if (ranksMap.containsKey(signInUserInfo.getId())) {
            UserPointsRankVO rankVO = ranksMap.get(signInUserInfo.getId());
            rankVO.setIsMe(1);
            return Lists.newArrayList(ranksMap.values());
        }

        // 如果不在 ranks 中，获取个人排名追加在最后
        // 获取排名
        Long myRank = redisTemplate.opsForZSet().reverseRank(
                RedisKeyConstant.user_points.getKey(), signInUserInfo.getId());
        if (myRank != null) {
            UserPointsRankVO me = new UserPointsRankVO();
            BeanUtils.copyProperties(signInUserInfo, me);
            me.setRanks(myRank.intValue() + 1);// 排名从 0 开始
            me.setIsMe(1);
            // 获取积分
            Double points = redisTemplate.opsForZSet().score(RedisKeyConstant.user_points.getKey(),
                    signInUserInfo.getId());
            me.setTotal(points.intValue());
            ranksMap.put(signInUserInfo.getId(), me);
        }
        return Lists.newArrayList(ranksMap.values());
    }

    /**
     * 查询前 20 积分排行榜，并显示个人排名 -- MySQL
     *
     * @param accessToken
     * @return
     */
    public List<UserPointsRankVO> findDinerPointRank(String accessToken) {
        // 获取登录用户信息
        SignInUserInfo SignInUserInfo = loadSignInUserInfo(accessToken);
        // 统计积分排行榜
        List<UserPointsRankVO> ranks = userPointsMapper.findTopN(TOPN);
        if (ranks == null || ranks.isEmpty()) {
            return Lists.newArrayList();
        }
        // 根据 key：用户 ID value：积分信息 构建一个 Map
        Map<Integer, UserPointsRankVO> ranksMap = new LinkedHashMap<>();
        for (int i = 0; i < ranks.size(); i++) {
            ranksMap.put(ranks.get(i).getId(), ranks.get(i));
        }
        // 判断个人是否在 ranks 中，如果在，添加标记直接返回
        if (ranksMap.containsKey(SignInUserInfo.getId())) {
            UserPointsRankVO myRank = ranksMap.get(SignInUserInfo.getId());
            myRank.setIsMe(1);
            return Lists.newArrayList(ranksMap.values());
        }
        // 如果不在 ranks 中，获取个人排名追加在最后
        UserPointsRankVO myRank = userPointsMapper.findUserRank(SignInUserInfo.getId());
        myRank.setIsMe(1);
        ranks.add(myRank);
        return ranks;
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
        SignInUserInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInUserInfo(), false);
        if (dinerInfo == null) {
            throw new ParameterException(ApiConstant.NO_LOGIN_CODE, ApiConstant.NO_LOGIN_MESSAGE);
        }
        return dinerInfo;
    }

}
