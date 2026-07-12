package com.xiyue.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiyue.order.entity.ServiceOrder;

/**
 * 订单 Mapper（MyBatis-Plus BaseMapper）。
 *
 * <p>抢单/取消等条件更新使用 {@link com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper}
 * 在 Service 层组装，保证原子性。
 */
public interface ServiceOrderMapper extends BaseMapper<ServiceOrder> {
}
