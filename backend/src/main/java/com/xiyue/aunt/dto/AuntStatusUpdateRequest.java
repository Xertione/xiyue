package com.xiyue.aunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员更新阿姨管理状态请求。
 */
@Data
@Schema(description = "管理员更新阿姨管理状态请求")
public class AuntStatusUpdateRequest {

    @Schema(description = "管理状态：AVAILABLE / OFF_SHELF / DISABLED", example = "AVAILABLE")
    @NotBlank(message = "管理状态不能为空")
    private String adminStatus;
}
