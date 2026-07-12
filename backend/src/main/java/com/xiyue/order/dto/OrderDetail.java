package com.xiyue.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单详情（含全部快照与支付/退款字段）。
 */
@Data
@Builder
public class OrderDetail {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long auntId;
    private LocalDate serviceDate;
    private Integer startHour;
    private Integer durationHours;
    private String contactName;
    private String contactPhone;
    private String address;
    private BigDecimal amount;
    private String auntName;
    private String auntAvatar;
    private Integer status;
    private String statusText;
    private String payNo;
    private LocalDateTime payTime;
    private String payMethod;
    private String refundStatus;
    private String refundNo;
    private LocalDateTime refundTime;
    private LocalDateTime cancelTime;
    private String completeImage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
