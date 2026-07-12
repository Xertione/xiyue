package com.xiyue.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiyue.order.entity.AuntBookingSlot;

/**
 * 阿姨档期占用 Mapper。
 *
 * <p>档期插入由抢单事务循环逐条 insert，冲突时抛 {@link org.springframework.dao.DuplicateKeyException}
 * 由 GlobalExceptionHandler 转为 1004；Service 层也可捕获后转为 AUNT_SLOT_CONFLICT(1003) 友好提示。
 * 档期释放按 order_id 批量 delete。
 */
public interface AuntBookingSlotMapper extends BaseMapper<AuntBookingSlot> {
}
