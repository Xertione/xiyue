package com.xiyue.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户投诉创建请求。
 */
@Data
public class ComplaintCreateRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "投诉原因不能为空")
    @Size(max = 500, message = "投诉原因不超过 500 字")
    private String reason;
}
