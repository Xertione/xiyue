package com.xiyue.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录成功响应。
 */
@Data
@Builder
@Schema(description = "登录成功响应")
public class LoginResponse {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色：USER / AUNT / ADMIN")
    private String role;

    @Schema(description = "昵称")
    private String nickname;
}
