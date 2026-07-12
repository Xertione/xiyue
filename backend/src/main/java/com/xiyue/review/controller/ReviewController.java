package com.xiyue.review.controller;

import com.xiyue.common.result.Result;
import com.xiyue.review.dto.ReviewCreateRequest;
import com.xiyue.review.dto.ReviewResponse;
import com.xiyue.review.service.ReviewService;
import com.xiyue.security.SecurityUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价接口（阶段 4）。
 */
@Tag(name = "评价模块", description = "用户评价创建与查看")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUserContext securityUserContext;

    @Operation(summary = "提交评价（仅待评价订单可评价一次）")
    @PostMapping("/api/reviews")
    @PreAuthorize("hasRole('USER')")
    public Result<ReviewResponse> create(@Valid @RequestBody ReviewCreateRequest req) {
        return Result.success(reviewService.create(securityUserContext.getCurrentUserId(), req));
    }

    @Operation(summary = "查看订单评价")
    @GetMapping("/api/orders/{id}/review")
    @PreAuthorize("isAuthenticated()")
    public Result<ReviewResponse> getByOrder(@PathVariable Long id) {
        return Result.success(reviewService.getByOrderId(id));
    }
}
