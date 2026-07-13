package com.xiyue.aunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 阿姨个人中心资料响应。
 */
@Data
@Builder
@Schema(description = "阿姨个人中心资料")
public class AuntProfileResponse {

    @Schema(description = "阿姨ID")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "标价")
    private BigDecimal price;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "入行年限")
    private Integer experience;

    @Schema(description = "技能标签")
    private String skillTags;

    @Schema(description = "个人介绍")
    private String intro;

    @Schema(description = "星级")
    private BigDecimal rating;

    @Schema(description = "服务次数")
    private Integer serviceCount;
}
