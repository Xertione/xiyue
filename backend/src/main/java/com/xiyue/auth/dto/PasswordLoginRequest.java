package com.xiyue.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 密码登录请求。
 */
@Data
@Schema(description = "密码登录请求")
public class PasswordLoginRequest {

    @Schema(description = "手机号（11位）", example = "13800000001")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "密码", example = "user123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
