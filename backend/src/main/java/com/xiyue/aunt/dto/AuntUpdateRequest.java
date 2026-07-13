package com.xiyue.aunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员编辑阿姨资料请求。
 *
 * <p>编辑运营字段（name/avatar/price/age/experience/skillTags/intro）；
 * rating/serviceCount 由系统维护；adminStatus/acceptStatus 通过独立接口更新。
 * 所有字段可选，做部分更新。
 */
@Data
@Schema(description = "管理员编辑阿姨资料请求")
public class AuntUpdateRequest {

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "标价（指定阿姨模式按此价计算）")
    @DecimalMin(value = "0.01", message = "标价至少 0.01 元")
    private BigDecimal price;

    @Schema(description = "年龄")
    @Min(value = 18, message = "年龄最小 18 岁")
    private Integer age;

    @Schema(description = "入行年限")
    @Min(value = 0, message = "入行年限不能为负")
    private Integer experience;

    @Schema(description = "技能标签（逗号分隔）")
    private String skillTags;

    @Schema(description = "个人介绍")
    private String intro;
}
