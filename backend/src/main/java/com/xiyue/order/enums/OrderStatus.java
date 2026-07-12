package com.xiyue.order.enums;

import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.ResultCode;
import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态枚举（9 状态机，ADR-003）。
 *
 * <p>状态流转：
 * <pre>
 * 待支付(0) ─支付─> 待抢单(1) ─抢单/指派─> 待服务(2) ─开始服务─> 服务中(3)
 *   └─取消─> 已取消(7)         └─取消─> 已取消(7)      └─取消─> 已取消(7)
 * 服务中(3) ─提交完成─> 待确认(4) ─用户确认─> 待评价(5) ─评价─> 已完成(6)
 *                                              └─投诉─> 投诉中(8) ─管理员处理─> 已完成(6)
 * </pre>
 *
 * <p>阶段 3 仅涉及：待支付 / 待抢单 / 待服务 / 已取消（取消范围）。
 * 服务中 / 待确认 / 待评价 / 已完成 / 投诉中 在阶段 4 服务履约与评价中实现。
 */
@Getter
public enum OrderStatus {

    PENDING_PAY(0, "待支付"),
    PENDING_GRAB(1, "待抢单"),
    PENDING_SERVICE(2, "待服务"),
    IN_SERVICE(3, "服务中"),
    PENDING_CONFIRM(4, "待确认"),
    PENDING_REVIEW(5, "待评价"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消"),
    COMPLAINT(8, "投诉中");

    private final int code;
    private final String message;

    OrderStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据 code 解析枚举，非法 code 抛业务异常。
     */
    public static OrderStatus fromCode(int code) {
        return Arrays.stream(values())
            .filter(s -> s.code == code)
            .findFirst()
            .orElseThrow(() -> new BusinessException(ResultCode.PARAM_INVALID, "非法订单状态码: " + code));
    }

    /**
     * 判断状态是否允许用户取消（规范 §7.6：仅待支付、待抢单、待服务允许取消）。
     */
    public static boolean isCancelable(int code) {
        return code == PENDING_PAY.code
            || code == PENDING_GRAB.code
            || code == PENDING_SERVICE.code;
    }

    /**
     * 判断取消时是否需要记录模拟退款。
     *
     * <p>已支付且未取消的订单（待抢单/待服务等）取消时需记录退款；
     * 待支付(0)未支付不需退款；已取消(7)不应再到达取消流程。
     *
     * @return true 需要记录模拟退款；false 不需要
     */
    public static boolean needsRefund(int code) {
        return code != PENDING_PAY.code && code != CANCELLED.code;
    }
}
