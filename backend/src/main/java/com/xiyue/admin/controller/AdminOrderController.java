package com.xiyue.admin.controller;

import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.Result;
import com.xiyue.order.dto.AssignRequest;
import com.xiyue.order.dto.OrderListItem;
import com.xiyue.order.service.OrderService;
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
 * 管理员订单接口（全量查看 + 指派阿姨兜底）。
 */
@Tag(name = "管理员-订单", description = "全量订单查看、指派阿姨")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "全量订单列表（可按状态筛选）")
    @GetMapping
    public Result<PageResponse<OrderListItem>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.success(orderService.listForAdmin(page, size, status));
    }

    @Operation(summary = "指派阿姨到待抢单订单（兜底机制）")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        orderService.assign(id, req.getAuntId());
        return Result.success();
    }
}
