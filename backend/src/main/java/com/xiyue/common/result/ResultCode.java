package com.xiyue.common.result;

import lombok.Getter;

/**
 * 响应码枚举。
 * 2xx 成功；4xx 客户端错误；5xx 服务端错误；1xxx 业务错误码。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    // 业务错误码 1xxx
    BUSINESS_ERROR(1000, "业务处理失败"),
    ORDER_STATUS_INVALID(1001, "订单当前状态不允许执行该操作"),
    ORDER_ALREADY_GRABBED(1002, "订单已被其他阿姨抢走"),
    AUNT_SLOT_CONFLICT(1003, "该阿姨在该时段已被预约，请选择其他阿姨"),
    DUPLICATE_KEY(1004, "数据冲突，请刷新后重试"),
    PARAM_INVALID(1005, "参数不合法"),

    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
