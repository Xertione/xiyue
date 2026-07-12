package com.xiyue.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约订单实体（含地址快照、价格快照、阿姨快照、支付与退款字段）。
 *
 * <p>快照设计（规范 §7.7）：创建订单时保存联系人/电话/地址/金额快照；
 * 抢单/指派成功时将阿姨姓名、头像写入订单快照。快照不随后续资料变更而变化。
 *
 * <p>金额字段 {@link #amount} 使用 {@link BigDecimal}，禁止 double（规范 §6）。
 */
@Data
@TableName("service_order")
public class ServiceOrder {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（全局唯一） */
    private String orderNo;

    /** 下单用户 sys_user.id */
    private Long userId;

    /** 抢单/指派阿姨 aunt.id（待支付/待抢单为 null） */
    private Long auntId;

    /** 服务日期 */
    private LocalDate serviceDate;

    /** 开始小时（0-23 整点） */
    private Integer startHour;

    /** 服务时长（小时，>=1） */
    private Integer durationHours;

    /** 联系人姓名（快照） */
    private String contactName;

    /** 联系电话（快照） */
    private String contactPhone;

    /** 服务地址（快照） */
    private String address;

    /** 订单金额（价格快照） */
    private BigDecimal amount;

    /** 阿姨姓名（抢单/指派时快照） */
    private String auntName;

    /** 阿姨头像（抢单/指派时快照） */
    private String auntAvatar;

    /** 订单状态：0待支付 1待抢单 2待服务 3服务中 4待确认 5待评价 6已完成 7已取消 8投诉中 */
    private Integer status;

    /** 模拟支付流水号 */
    private String payNo;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 支付方式（MOCK） */
    private String payMethod;

    /** 退款状态：REFUNDED 模拟退款成功 */
    private String refundStatus;

    /** 模拟退款流水号 */
    private String refundNo;

    /** 模拟退款时间 */
    private LocalDateTime refundTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
