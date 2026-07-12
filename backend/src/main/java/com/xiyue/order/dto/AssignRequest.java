package com.xiyue.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员指派阿姨请求（兜底机制，规范 §4 管理员职责）。
 */
@Data
public class AssignRequest {

    /** 被指派的阿姨 aunt.id */
    @NotNull(message = "阿姨ID不能为空")
    private Long auntId;
}
