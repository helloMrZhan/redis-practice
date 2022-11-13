package com.zjq.seckill.mapper;

import com.zjq.commons.model.pojo.SeckillVouchers;
import org.apache.ibatis.annotations.*;

/**
 * 秒杀代金券 Mapper
 * @author zjq
 */
public interface SeckillVouchersMapper {

    /**
     * 新增秒杀活动
     * @param seckillVouchers 代金券实体
     * @return
     */
    @Insert("insert into t_seckill_vouchers (fk_voucher_id, amount, start_time, end_time, is_valid, create_date, update_date) " +
            " values (#{fkVoucherId}, #{amount}, #{startTime}, #{endTime}, 1, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(SeckillVouchers seckillVouchers);

    /**
     * 根据代金券 ID 查询该代金券是否参与抢购活动
     * @param voucherId 代金券id
     * @return
     */
    @Select("select id, fk_voucher_id, amount, start_time, end_time, is_valid " +
            " from t_seckill_vouchers where fk_voucher_id = #{voucherId}")
    SeckillVouchers selectVoucher(Integer voucherId);

    /**
     * 减库存
     * @param seckillId 秒杀id
     * @return
     */
    @Update("update t_seckill_vouchers set amount = amount - 1 " +
            " where id = #{seckillId}")
    int stockDecrease(@Param("seckillId") int seckillId);

}
