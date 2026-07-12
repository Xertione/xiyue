package com.xiyue.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 抢单大厅列表项（阿姨视角，待抢单订单）。
 *
 * <p>含联系人/电话/地址，便于阿姨决定是否抢单。不含已抢单的阿姨信息（待抢单 aunt_id 为 null）。
 */
@Data
@Builder
public class GrabListItem {

    private Long id;
    private String orderNo;
    private LocalDate serviceDate;
    private Integer startHour;
    private Integer durationHours;
    private String address;
    private BigDecimal amount;
    private String contactName;
    private String contactPhone;
    private LocalDateTime createTime;
}
