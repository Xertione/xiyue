package com.xiyue.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统账号实体（用户 / 阿姨 / 管理员共用）。
 *
 * <p>角色通过 {@link #role} 字段区分，密码使用 BCrypt 哈希存储，
 * 手机号全局唯一（数据库唯一索引 uk_phone 保障）。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（11 位，全局唯一） */
    private String phone;

    /** BCrypt 密码哈希 */
    private String password;

    /** 角色：USER / AUNT / ADMIN */
    private String role;

    /** 昵称（可空） */
    private String nickname;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
