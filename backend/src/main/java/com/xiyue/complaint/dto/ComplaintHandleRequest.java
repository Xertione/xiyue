package com.xiyue.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员处理投诉请求。
 */
@Data
public class ComplaintHandleRequest {

    @NotBlank(message = "处理备注不能为空")
    private String handleRemark;
}
