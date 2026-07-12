package com.xiyue.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单列表项（用户/管理员/阿姨订单列表通用）。
 */
@Data
@Builder
public class OrderListItem {

    private Long id;
    private String orderNo;
    private LocalDate serviceDate;
    private Integer startHour;
    private Integer durationHours;
    private Integer status;
    private String statusText;
    private BigDecimal amount;
    private String auntName;
    private LocalDateTime createTime;
}
