package com.xiyue.order.controller;

import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.Result;
import com.xiyue.order.dto.CompleteRequest;
import com.xiyue.order.dto.CreateOrderRequest;
import com.xiyue.order.dto.GrabListItem;
import com.xiyue.order.dto.OrderDetail;
import com.xiyue.order.dto.OrderListItem;
import com.xiyue.order.dto.PayResponse;
import com.xiyue.order.service.OrderService;
import com.xiyue.security.LoginUser;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口（用户端 + 阿姨端共用前缀 /api/orders）。
 *
 * <p>角色隔离通过 @PreAuthorize 控制：
 * <ul>
 *   <li>USER：创建/支付/取消/列表/详情；</li>
 *   <li>AUNT：抢单大厅/抢单/我的订单/详情。</li>
 * </ul>
 *
 * <p>GET /{id} 详情接口允许 USER 与 AUNT 访问，Controller 按角色分流到对应 Service 方法，
 * 确保归属校验正确（USER 看 user_id，AUNT 看 aunt_id）。
 */
@Tag(name = "订单模块", description = "用户创建/支付/取消订单，阿姨抢单/我的订单")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUserContext securityUserContext;

    // ===== 用户端 =====

    @Operation(summary = "创建订单")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<OrderDetail> create(@Valid @RequestBody CreateOrderRequest req) {
        Long userId = securityUserContext.getCurrentUserId();
        return Result.success(orderService.createOrder(userId, req));
    }

    @Operation(summary = "用户订单列表")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResponse<OrderListItem>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        Long userId = securityUserContext.getCurrentUserId();
        return Result.success(orderService.listForUser(userId, page, size, status));
    }

    @Operation(summary = "订单详情（USER 查自己下单的，AUNT 查自己接的）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','AUNT')")
    public Result<OrderDetail> detail(@PathVariable Long id) {
        LoginUser user = securityUserContext.getCurrentUser();
        // 按角色分流，确保归属校验正确
        if ("AUNT".equals(user.getRole())) {
            return Result.success(orderService.getDetailForAunt(user.getUserId(), id));
        }
        return Result.success(orderService.getDetailForUser(user.getUserId(), id));
    }

    @Operation(summary = "模拟支付")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('USER')")
    public Result<PayResponse> pay(@PathVariable Long id) {
        Long userId = securityUserContext.getCurrentUserId();
        return Result.success(orderService.pay(userId, id));
    }

    @Operation(summary = "取消订单（待支付/待抢单/待服务可取消，含模拟退款与档期释放）")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = securityUserContext.getCurrentUserId();
        orderService.cancel(userId, id);
        return Result.success();
    }

    @Operation(summary = "用户确认服务完成（待确认→待评价）")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> confirm(@PathVariable Long id) {
        Long userId = securityUserContext.getCurrentUserId();
        orderService.confirm(userId, id);
        return Result.success();
    }

    // ===== 阿姨端 =====

    @Operation(summary = "抢单大厅（待抢单订单列表）")
    @GetMapping("/grab-list")
    @PreAuthorize("hasRole('AUNT')")
    public Result<PageResponse<GrabListItem>> grabList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        Long auntUserId = securityUserContext.getCurrentUserId();
        return Result.success(orderService.grabList(auntUserId, page, size));
    }

    @Operation(summary = "阿姨抢单")
    @PostMapping("/{id}/grab")
    @PreAuthorize("hasRole('AUNT')")
    public Result<Void> grab(@PathVariable Long id) {
        Long auntUserId = securityUserContext.getCurrentUserId();
        orderService.grab(auntUserId, id);
        return Result.success();
    }

    @Operation(summary = "阿姨订单列表（我接的订单）")
    @GetMapping("/mine")
    @PreAuthorize("hasRole('AUNT')")
    public Result<PageResponse<OrderListItem>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        Long auntUserId = securityUserContext.getCurrentUserId();
        return Result.success(orderService.listForAunt(auntUserId, page, size, status));
    }

    @Operation(summary = "阿姨开始服务（待服务→服务中）")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('AUNT')")
    public Result<Void> start(@PathVariable Long id) {
        Long auntUserId = securityUserContext.getCurrentUserId();
        orderService.start(auntUserId, id);
        return Result.success();
    }

    @Operation(summary = "阿姨提交服务完成（服务中→待确认，上传演示图片URL）")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('AUNT')")
    public Result<Void> complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest req) {
        Long auntUserId = securityUserContext.getCurrentUserId();
        orderService.complete(auntUserId, id, req.getImageUrl());
        return Result.success();
    }
}
