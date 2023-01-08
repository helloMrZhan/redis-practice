package com.zjq.feeds.controller;


import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.pojo.Feeds;
import com.zjq.commons.model.vo.FeedsVO;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.feeds.service.FeedsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Feed控制类
 * @author zjq
 */
@RestController
public class FeedsController {

    @Resource
    private FeedsService feedsService;
    @Resource
    private HttpServletRequest request;

    /**
     * 分页获取关注的 Feed 数据
     *
     * @param page
     * @param access_token
     * @return
     */
    @GetMapping("{page}")
    public ResultInfo selectForPage(@PathVariable Integer page, String access_token) {
        List<FeedsVO> feedsVOS = feedsService.selectForPage(page, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), feedsVOS);
    }

    /**
     * 变更 Feed
     *
     * @return
     */
    @PostMapping("updateFollowingFeeds/{followinguserId}")
    public ResultInfo addFollowingFeeds(@PathVariable Integer followinguserId,
                                        String access_token, @RequestParam int type) {
        feedsService.addFollowingFeed(followinguserId, access_token, type);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "操作成功");
    }

    /**
     * 删除 Feed
     *
     * @param id
     * @param access_token
     * @return
     */
    @DeleteMapping("{id}")
    public ResultInfo delete(@PathVariable Integer id, String access_token) {
        feedsService.delete(id, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "删除成功");
    }

    /**
     * 添加 Feed
     *
     * @param feeds
     * @param access_token
     * @return
     */
    @PostMapping
    public ResultInfo<String> create(@RequestBody Feeds feeds, String access_token) {
        feedsService.create(feeds, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "添加成功");
    }

}
