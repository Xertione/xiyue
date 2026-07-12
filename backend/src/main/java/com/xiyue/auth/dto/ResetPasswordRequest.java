package com.xiyue.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 找回密码请求。
 */
@Data
@Schema(description = "找回密码请求")
public class ResetPasswordRequest {

    @Schema(description = "手机号（11位）", example = "13800000001")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "短信验证码", example = "123456")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @Schema(description = "新密码（6-20位）", example = "newpass123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String newPassword;
}
