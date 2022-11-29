package com.zjq.seckill.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.zjq.commons.constant.ApiConstant;
import com.zjq.commons.constant.RedisKeyConstant;
import com.zjq.commons.exception.ParameterException;
import com.zjq.commons.model.domain.ResultInfo;
import com.zjq.commons.model.pojo.SeckillVouchers;
import com.zjq.commons.model.pojo.VoucherOrders;
import com.zjq.commons.model.vo.SignInDinerInfo;
import com.zjq.commons.utils.AssertUtil;
import com.zjq.commons.utils.ResultInfoUtil;
import com.zjq.seckill.mapper.SeckillVouchersMapper;
import com.zjq.seckill.mapper.VoucherOrdersMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀业务逻辑层
 *
 * @author zjq
 */
@Service
public class SeckillService {

    @Resource
    private SeckillVouchersMapper seckillVouchersMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private VoucherOrdersMapper voucherOrdersMapper;
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    DefaultRedisScript defaultRedisScript;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 添加需要抢购的代金券
     *
     * @param seckillVouchers
     */
    @Transactional(rollbackFor = Exception.class)
    public void addSeckillVouchers(SeckillVouchers seckillVouchers) {
        // 非空校验
        AssertUtil.isTrue(seckillVouchers.getFkVoucherId() == null, "请选择需要抢购的代金券");
        AssertUtil.isTrue(seckillVouchers.getAmount() == 0, "请输入抢购总数量");
        Date now = new Date();
        AssertUtil.isNotNull(seckillVouchers.getStartTime(), "请输入开始时间");
        // 生产环境下面一行代码需放行，这里注释方便测试
        // AssertUtil.isTrue(now.after(seckillVouchers.getStartTime()), "开始时间不能早于当前时间");
        AssertUtil.isNotNull(seckillVouchers.getEndTime(), "请输入结束时间");
        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "结束时间不能早于当前时间");
        AssertUtil.isTrue(seckillVouchers.getStartTime().after(seckillVouchers.getEndTime()), "开始时间不能晚于结束时间");
        // 注释原始的走 关系型数据库 的流程
        // 验证数据库中是否已经存在该券的秒杀活动
//         SeckillVouchers seckillVouchersFromDb = seckillVouchersMapper.selectVoucher(seckillVouchers.getFkVoucherId());
//         AssertUtil.isTrue(seckillVouchersFromDb != null, "该券已经拥有了抢购活动");
////         插入数据库
//         seckillVouchersMapper.save(seckillVouchers);

        // 采用 Redis 实现
        String key = RedisKeyConstant.seckill_vouchers.getKey() +
                seckillVouchers.getFkVoucherId();
        // 验证 Redis 中是否已经存在该券的秒杀活动
        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
        AssertUtil.isTrue(!map.isEmpty() && (int) map.get("amount") > 0, "该券已经拥有了抢购活动");

        // 插入 Redis
        seckillVouchers.setIsValid(1);
        seckillVouchers.setCreateDate(now);
        seckillVouchers.setUpdateDate(now);
        redisTemplate.opsForHash().putAll(key, BeanUtil.beanToMap(seckillVouchers));
    }

    /**
     * 抢购代金券
     *
     * @param voucherId   代金券 ID
     * @param accessToken 登录token
     * @Para path 访问路径
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultInfo doSeckill(Integer voucherId, String accessToken, String path) {
        // 基本参数校验
        AssertUtil.isTrue(voucherId == null || voucherId < 0, "请选择需要抢购的代金券");
        AssertUtil.isNotEmpty(accessToken, "请登录");
        // 注释原始的 关系型数据库 的流程
        // 判断此代金券是否加入抢购
//        SeckillVouchers seckillVouchers = seckillVouchersMapper.selectVoucher(voucherId);
//        AssertUtil.isTrue(seckillVouchers == null, "该代金券并未有抢购活动");
//        // 判断是否有效
//        AssertUtil.isTrue(seckillVouchers.getIsValid() == 0, "该活动已结束");

        // 采用 Redis
        String key = RedisKeyConstant.seckill_vouchers.getKey() + voucherId;
        Map<String, Object> map = redisTemplate.opsForHash().entries(key);
        SeckillVouchers seckillVouchers = BeanUtil.mapToBean(map, SeckillVouchers.class, true, null);
        // 判断是否开始、结束
        Date now = new Date();
        AssertUtil.isTrue(now.before(seckillVouchers.getStartTime()), "该抢购还未开始");
        AssertUtil.isTrue(now.after(seckillVouchers.getEndTime()), "该抢购已结束");
        // 判断是否卖完
        AssertUtil.isTrue(seckillVouchers.getAmount() < 1, "该券已经卖完了");
        // 获取登录用户信息
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            resultInfo.setPath(path);
            return resultInfo;
        }
        // 这里的data是一个LinkedHashMap，SignInDinerInfo
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInDinerInfo(), false);
        // 判断登录用户是否已抢到(一个用户针对这次活动只能买一次)
        // 注释传统数据库判定方式
//        VoucherOrders order = voucherOrdersMapper.findDinerOrder(dinerInfo.getId(),
//                seckillVouchers.getId());
//        AssertUtil.isTrue(order != null, "该用户已抢到该代金券，无需再抢");

        // 使用 Redis 锁一个账号只能购买一次
        String lockName = RedisKeyConstant.lock_key.getKey()
                + dinerInfo.getId() + ":" + voucherId;
        long expireTime = seckillVouchers.getEndTime().getTime() - now.getTime();

        // Redisson 分布式锁
        RLock lock = redissonClient.getLock(lockName);
        try {
            // 不为空意味着拿到锁了，执行下单
            // 自定义 Redis 分布式锁处理
            // if (StrUtil.isNotBlank(lockKey)) {

            // Redisson 分布式锁处理
            boolean isLocked = lock.tryLock(expireTime, TimeUnit.MILLISECONDS);
            if (isLocked) {
                // 下单
                VoucherOrders voucherOrders = new VoucherOrders();
                voucherOrders.setFkDinerId(dinerInfo.getId());
                //redis中不需要维护该外键信息
            //        voucherOrders.setFkSeckillId(seckillVouchers.getId());
                voucherOrders.setFkVoucherId(seckillVouchers.getFkVoucherId());
                String orderNo = IdUtil.getSnowflake(1, 1).nextIdStr();
                voucherOrders.setOrderNo(orderNo);
                voucherOrders.setOrderType(1);
                voucherOrders.setStatus(0);
                long count = voucherOrdersMapper.save(voucherOrders);
                AssertUtil.isTrue(count == 0, "用户抢购失败");

                // 扣库存
                // 注释原始的 关系型数据库 的流程
            //        int count = seckillVouchersMapper.stockDecrease(seckillVouchers.getId());
                // 扣库存 redis没有自减方法，数值传负数表示自减
            //        count = redisTemplate.opsForHash().increment(key, "amount", -1);
            //        AssertUtil.isTrue(count < 0, "该券已经卖完了");

                // 采用 Redis + Lua 解决超卖问题
                // 扣库存
                List<String> keys = new ArrayList<>();
                keys.add(key);
                keys.add("amount");
                Long amount = (Long) redisTemplate.execute(defaultRedisScript, keys);
                AssertUtil.isTrue(amount == null || amount < 1, "该券已经卖完了");
            }
        } catch (Exception e) {
            // 手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            // Redisson 解锁
            lock.unlock();
            if (e instanceof ParameterException) {
                return ResultInfoUtil.buildError(0, "该券已经卖完了", path);
            }
        }

        return ResultInfoUtil.buildSuccess(path, "抢购成功");
    }


}
