package com.xiyue.common.enums;

/**
 * 系统角色枚举。
 *
 * <p>三种角色共用 sys_user 表，通过 role 字段区分：
 * <ul>
 *   <li>USER  普通用户（下单、支付、评价、投诉）</li>
 *   <li>AUNT  阿姨（自行注册、抢单、履约）</li>
 *   <li>ADMIN 管理员（仅由系统初始化创建，禁止公开注册）</li>
 * </ul>
 */
public enum RoleEnum {

    USER,
    AUNT,
    ADMIN;

    /**
     * 判断角色字符串是否合法且允许公开注册（仅 USER / AUNT）。
     * ADMIN 不允许通过注册接口创建。
     */
    public static boolean isRegisterable(String role) {
        if (role == null) {
            return false;
        }
        return USER.name().equals(role) || AUNT.name().equals(role);
    }
}
