package com.xiyue.order.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 模拟支付响应。
 */
@Data
@Builder
public class PayResponse {

    private String orderNo;
    private String payNo;
    private Integer status;
    private String statusText;
}
