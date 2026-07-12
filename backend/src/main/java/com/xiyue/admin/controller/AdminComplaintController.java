package com.xiyue.admin.controller;

import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.Result;
import com.xiyue.complaint.dto.ComplaintHandleRequest;
import com.xiyue.complaint.dto.ComplaintResponse;
import com.xiyue.complaint.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员投诉接口（阶段 4）。
 */
@Tag(name = "管理员-投诉", description = "查看投诉列表、处理投诉")
@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @Operation(summary = "投诉列表（可按状态筛选 PENDING/HANDLED）")
    @GetMapping
    public Result<PageResponse<ComplaintResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return Result.success(complaintService.listForAdmin(page, size, status));
    }

    @Operation(summary = "处理投诉（订单变为已完成，不再允许评价，不更新阿姨评分）")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody ComplaintHandleRequest req) {
        complaintService.handle(id, req);
        return Result.success();
    }
}
