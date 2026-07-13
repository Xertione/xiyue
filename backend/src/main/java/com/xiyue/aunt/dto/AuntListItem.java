package com.xiyue.aunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 阿姨列表项（用户端 / 管理员端列表共用）。
 */
@Data
@Builder
@Schema(description = "阿姨列表项")
public class AuntListItem {

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

    @Schema(description = "星级（0.0-5.0）")
    private BigDecimal rating;

    @Schema(description = "服务次数")
    private Integer serviceCount;

    @Schema(description = "技能标签（逗号分隔）")
    private String skillTags;

    @Schema(description = "个人介绍")
    private String intro;
}
