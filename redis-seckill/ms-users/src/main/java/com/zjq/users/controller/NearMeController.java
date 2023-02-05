package com.zjq.users.controller;

import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.vo.NearMeUserVO;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.users.service.NearMeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 附近的人控制层
 * @author zjq
 */
@RestController
@RequestMapping("nearMe")
public class NearMeController {

    @Resource
    private HttpServletRequest request;
    @Resource
    private NearMeService nearMeService;

    /**
     * 新增/更新用户坐标
     *
     * @param access_token
     * @param lon
     * @param lat
     * @return
     */
    @PostMapping
    public ResultInfo saveOrUpdateUserLocation(String access_token,
                                         @RequestParam Float lon,
                                         @RequestParam Float lat) {
        nearMeService.saveOrUpdateUserLocation(access_token, lon, lat);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "更新成功");
    }

    /**
     * 获取附近的人
     *
     * @param access_token
     * @param radius
     * @param lon
     * @param lat
     * @return
     */
    @GetMapping
    public ResultInfo nearMe(String access_token,
                             Integer radius,
                             Float lon, Float lat) {
        List<NearMeUserVO> nearMe = nearMeService.findNearMe(access_token, radius, lon, lat);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), nearMe);
    }

}
