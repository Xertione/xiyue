package com.xiyue.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求。
 *
 * <p>角色仅允许 USER / AUNT，由后端白名单校验；ADMIN 禁止通过此接口创建。
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(description = "手机号（11位）", example = "13800000001")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "密码（6-20位）", example = "user123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;

    @Schema(description = "短信验证码", example = "123456")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @Schema(description = "注册角色：USER 或 AUNT（ADMIN 禁止注册）", example = "USER")
    @NotBlank(message = "角色不能为空")
    private String role;
}
