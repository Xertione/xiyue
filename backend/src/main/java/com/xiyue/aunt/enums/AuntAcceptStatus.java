package com.xiyue.aunt.enums;

/**
 * 阿姨接单状态枚举（阿姨自主控制，ADR-008）。
 *
 * <ul>
 *   <li>AVAILABLE  可抢单（出现在抢单大厅可抢单）</li>
 *   <li>RESTING    休息（暂不接单，不影响已有订单）</li>
 * </ul>
 *
 * <p>与管理状态 {@link AuntAdminStatus} 相互独立；档期占用由 aunt_booking_slot 表管理（阶段3）。
 */
public enum AuntAcceptStatus {

    AVAILABLE,
    RESTING;

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (AuntAcceptStatus s : values()) {
            if (s.name().equals(status)) {
                return true;
            }
        }
        return false;
    }
}
