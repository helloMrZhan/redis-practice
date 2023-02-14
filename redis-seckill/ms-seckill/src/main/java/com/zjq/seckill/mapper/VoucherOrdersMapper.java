package com.zjq.seckill.mapper;

import com.zjq.commons.model.pojo.VoucherOrders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 代金券订单 Mapper
 * @author zjq
 */
public interface VoucherOrdersMapper {

    /**
     * 根据用户 ID 和秒杀 ID 查询代金券订单
     * @param userId
     * @param voucherId
     * @return
     */
    @Select("select id, order_no, fk_voucher_id, fk_user_id, qrcode, payment," +
            " status, fk_seckill_id, order_type, create_date, update_date, " +
            " is_valid from t_voucher_orders where fk_user_id = #{userId} " +
            " and fk_voucher_id = #{voucherId} and is_valid = 1 and status between 0 and 1 ")
    VoucherOrders findUserOrder(@Param("userId") Integer userId,
                                 @Param("voucherId") Integer voucherId);

    /**
     * 新增代金券订单
     * @param voucherOrders 代金券实体
     * @return
     */
    @Insert("insert into t_voucher_orders (order_no, fk_voucher_id, fk_user_id, " +
            " status, fk_seckill_id, order_type, create_date, update_date,  is_valid)" +
            " values (#{orderNo}, #{fkVoucherId}, #{fkuserId}, #{status}, #{fkSeckillId}, " +
            " #{orderType}, now(), now(), 1)")
    int save(VoucherOrders voucherOrders);

}
