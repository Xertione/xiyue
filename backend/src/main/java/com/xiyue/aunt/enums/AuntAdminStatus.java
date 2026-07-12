package com.xiyue.aunt.enums;

/**
 * 阿姨管理状态枚举（管理员控制，ADR-008）。
 *
 * <ul>
 *   <li>AVAILABLE  可用（用户可见，可接单）</li>
 *   <li>OFF_SHELF  下架（用户不可见，管理员主动下架）</li>
 *   <li>DISABLED   禁用（用户不可见，违规封禁）</li>
 * </ul>
 *
 * <p>用户端阿姨列表只展示 {@code AVAILABLE}；{@code OFF_SHELF}/{@code DISABLED} 对用户不可见。
 * 与接单状态 {@link AuntAcceptStatus} 相互独立，禁止混用。
 */
public enum AuntAdminStatus {

    AVAILABLE,
    OFF_SHELF,
    DISABLED;

    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (AuntAdminStatus s : values()) {
            if (s.name().equals(status)) {
                return true;
            }
        }
        return false;
    }
}
