package com.xiyue.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 通用分页响应。
 *
 * @param <T> 记录类型
 */
@Data
@Builder
@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "当前页记录列表")
    private List<T> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码（从 1 开始）")
    private Long page;

    @Schema(description = "每页大小")
    private Long size;
}
