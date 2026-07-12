package com.xiyue.complaint.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投诉响应。
 */
@Data
@Builder
public class ComplaintResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private String reason;
    private String status;
    private String handleRemark;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
