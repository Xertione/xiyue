package com.xiyue.review.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价响应。
 */
@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
}
