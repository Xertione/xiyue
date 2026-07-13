package com.xiyue.aunt.controller;

import com.xiyue.aunt.dto.AuntAcceptStatusRequest;
import com.xiyue.aunt.dto.AuntDetail;
import com.xiyue.aunt.dto.AuntListItem;
import com.xiyue.aunt.dto.AuntProfileResponse;
import com.xiyue.aunt.dto.UpdateAuntProfileRequest;
import com.xiyue.aunt.service.AuntService;
import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.Result;
import com.xiyue.security.SecurityUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 阿姨模块接口（用户端 + 阿姨自己）。
 *
 * <p>路径前缀 {@code /api/aunts}：
 * <ul>
 *   <li>GET /       用户端阿姨列表（USER）</li>
 *   <li>GET /{id}   用户端阿姨详情（USER）</li>
 *   <li>PATCH /me/status  阿姨设接单状态（AUNT）</li>
 * </ul>
 * 管理员接口见 {@link com.xiyue.admin.controller.AdminAuntController}。
 */
@RestController
@RequestMapping("/api/aunts")
@RequiredArgsConstructor
@Tag(name = "阿姨模块-用户端", description = "用户浏览阿姨列表与详情、阿姨设置接单状态")
public class AuntController {

    private final AuntService auntService;
    private final SecurityUserContext securityUserContext;

    @Operation(summary = "阿姨列表", description = "仅展示 AVAILABLE 阿姨，支持星级/价格/技能标签筛选与排序分页")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResponse<AuntListItem>> list(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) BigDecimal minRating,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String skillTag,
        @RequestParam(required = false) String sort) {
        return Result.success(auntService.listForUser(page, size, minRating, minPrice, maxPrice, skillTag, sort));
    }

    @Operation(summary = "阿姨详情", description = "仅 AVAILABLE 阿姨可见，下架/禁用返回不存在")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Result<AuntDetail> detail(@PathVariable Long id) {
        return Result.success(auntService.getDetailForUser(id));
    }

    @Operation(summary = "阿姨设置个人接单状态", description = "AVAILABLE 可抢单 / RESTING 休息")
    @PatchMapping("/me/status")
    @PreAuthorize("hasRole('AUNT')")
    public Result<Void> updateMyAcceptStatus(@Valid @RequestBody AuntAcceptStatusRequest req) {
        Long userId = securityUserContext.getCurrentUserId();
        auntService.updateMyAcceptStatus(userId, req.getAcceptStatus());
        return Result.success();
    }

    @Operation(summary = "获取个人资料", description = "阿姨查看自己的个人资料")
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('AUNT')")
    @Tag(name = "阿姨-个人中心")
    public Result<AuntProfileResponse> getMyProfile() {
        Long userId = securityUserContext.getCurrentUserId();
        return Result.success(auntService.getMyProfile(userId));
    }

    @Operation(summary = "更新个人资料", description = "阿姨自助编辑个人资料，仅更新非空字段")
    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('AUNT')")
    @Tag(name = "阿姨-个人中心")
    public Result<AuntProfileResponse> updateMyProfile(@Valid @RequestBody UpdateAuntProfileRequest req) {
        Long userId = securityUserContext.getCurrentUserId();
        return Result.success(auntService.updateMyProfile(userId, req));
    }
}
