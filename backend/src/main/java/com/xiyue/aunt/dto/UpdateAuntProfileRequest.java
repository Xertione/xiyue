package com.xiyue.aunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 阿姨自助编辑个人资料请求。
 *
 * <p>所有字段可选，仅更新非空字段。
 */
@Data
@Schema(description = "阿姨自助编辑个人资料请求")
public class UpdateAuntProfileRequest {

    @Size(max = 50, message = "姓名最多 50 字")
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像URL")
    private String avatar;

    @DecimalMin(value = "0.01", message = "标价至少 0.01 元")
    @Schema(description = "标价")
    private BigDecimal price;

    @Min(value = 18, message = "年龄最小 18 岁")
    @Schema(description = "年龄")
    private Integer age;

    @Min(value = 0, message = "入行年限不能为负")
    @Schema(description = "入行年限")
    private Integer experience;

    @Schema(description = "技能标签（逗号分隔）")
    private String skillTags;

    @Schema(description = "个人介绍")
    private String intro;
}
