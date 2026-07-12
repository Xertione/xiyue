package com.xiyue.admin.controller;

import com.xiyue.aunt.dto.AuntDetail;
import com.xiyue.aunt.dto.AuntListItem;
import com.xiyue.aunt.dto.AuntStatusUpdateRequest;
import com.xiyue.aunt.dto.AuntUpdateRequest;
import com.xiyue.aunt.service.AuntService;
import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 阿姨模块接口（管理员端）。
 *
 * <p>路径前缀 {@code /api/admin/aunts}，全部需要 ADMIN 角色：
 * <ul>
 *   <li>GET /              全量阿姨列表（含所有管理状态）</li>
 *   <li>GET /{id}          阿姨详情</li>
 *   <li>PUT /{id}          编辑阿姨运营字段</li>
 *   <li>DELETE /{id}       逻辑删除阿姨</li>
 *   <li>PATCH /{id}/status 更新管理状态（上下架/禁用）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/aunts")
@RequiredArgsConstructor
@Tag(name = "阿姨模块-管理员端", description = "管理员管理阿姨资料、状态与逻辑删除")
public class AdminAuntController {

    private final AuntService auntService;

    @Operation(summary = "全量阿姨列表", description = "含所有管理状态，已逻辑删除的自动过滤")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<AuntListItem>> list(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size) {
        return Result.success(auntService.listForAdmin(page, size));
    }

    @Operation(summary = "阿姨详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AuntDetail> detail(@PathVariable Long id) {
        return Result.success(auntService.getDetailForAdmin(id));
    }

    @Operation(summary = "编辑阿姨资料", description = "仅运营字段：name/avatar/price/skillTags/intro")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AuntUpdateRequest req) {
        auntService.updateByAdmin(id, req);
        return Result.success();
    }

    @Operation(summary = "逻辑删除阿姨", description = "标记 deleted=1，记录保留；存在历史订单的物理删除检查在阶段3补")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        auntService.deleteByAdmin(id);
        return Result.success();
    }

    @Operation(summary = "更新阿姨管理状态", description = "AVAILABLE 可用 / OFF_SHELF 下架 / DISABLED 禁用")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody AuntStatusUpdateRequest req) {
        auntService.updateAdminStatus(id, req.getAdminStatus());
        return Result.success();
    }
}
