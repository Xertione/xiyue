package com.xiyue.complaint.controller;

import com.xiyue.common.result.Result;
import com.xiyue.complaint.dto.ComplaintCreateRequest;
import com.xiyue.complaint.dto.ComplaintResponse;
import com.xiyue.complaint.service.ComplaintService;
import com.xiyue.security.SecurityUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投诉接口（用户端，阶段 4）。
 */
@Tag(name = "投诉模块", description = "用户提交投诉")
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final SecurityUserContext securityUserContext;

    @Operation(summary = "提交投诉（仅待评价订单可投诉一次）")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<ComplaintResponse> create(@Valid @RequestBody ComplaintCreateRequest req) {
        return Result.success(complaintService.create(securityUserContext.getCurrentUserId(), req));
    }
}
