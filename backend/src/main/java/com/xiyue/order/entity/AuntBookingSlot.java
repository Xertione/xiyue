package com.xiyue.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 阿姨小时块档期占用记录实体。
 *
 * <p>联合唯一索引 uk_aunt_date_hour (aunt_id, service_date, hour_slot) 保障
 * 同一阿姨同一日期同一小时块只能被一单占用（ADR-002、ADR-010）。
 *
 * <p>抢单/指派成功时插入；取消时按 order_id 删除释放。
 * 待支付/待抢单订单不插入档期记录（规范 §5.3）。
 */
@Data
@TableName("aunt_booking_slot")
public class AuntBookingSlot {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 占用档期的阿姨 aunt.id */
    private Long auntId;

    /** 关联订单 service_order.id */
    private Long orderId;

    /** 服务日期 */
    private LocalDate serviceDate;

    /** 小时块（0-23，整点） */
    private Integer hourSlot;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
