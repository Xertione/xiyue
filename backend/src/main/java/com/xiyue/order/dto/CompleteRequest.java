package com.xiyue.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 阿姨提交服务完成请求（含演示图片 URL，规范 §9 阶段4）。
 */
@Data
public class CompleteRequest {

    /** 服务完成演示图片 URL */
    @NotBlank(message = "完成图片URL不能为空")
    @Pattern(regexp = "^(https?://\\S+|/\\S+)$", message = "完成图片URL格式不正确")
    private String imageUrl;
}
