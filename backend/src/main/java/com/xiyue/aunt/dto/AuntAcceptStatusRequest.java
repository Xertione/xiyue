package com.xiyue.aunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 阿姨更新个人接单状态请求。
 */
@Data
@Schema(description = "阿姨更新接单状态请求")
public class AuntAcceptStatusRequest {

    @Schema(description = "接单状态：AVAILABLE / RESTING", example = "RESTING")
    @NotBlank(message = "接单状态不能为空")
    private String acceptStatus;
}
