package com.zjq.points.controller;


import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.vo.UserPointsRankVO;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.points.service.UserPointsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 积分控制层
 * @author zjq
 */
@RestController
public class UserPointsController {

    @Resource
    private UserPointsService userPointsService;
    @Resource
    private HttpServletRequest request;

    /**
     * 添加积分
     *
     * @param userId 用户ID
     * @param points  积分
     * @param types   类型 0=签到，1=关注好友，2=添加Feed，3=添加商户评论
     * @return
     */
    @PostMapping
    public ResultInfo<Integer> addPoints(@RequestParam(required = false) Integer userId,
                                         @RequestParam(required = false) Integer points,
                                         @RequestParam(required = false) Integer types) {
        userPointsService.addPoints(userId, points, types);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), points);
    }

    /**
     * 查询前 20 积分排行榜，同时显示用户排名 -- Redis
     *
     * @param access_token
     * @return
     */
    @GetMapping("redis")
    public ResultInfo findUserPointsRankFromRedis(String access_token) {
        List<UserPointsRankVO> ranks = userPointsService.findUserPointRankFromRedis(access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), ranks);
    }

    /**
     * 查询前 20 积分排行榜，同时显示用户排名 -- MySQL
     *
     * @param access_token
     * @return
     */
    @GetMapping
    public ResultInfo findUserPointsRank(String access_token) {
        List<UserPointsRankVO> ranks = userPointsService.findUserPointRank(access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), ranks);
    }

}
