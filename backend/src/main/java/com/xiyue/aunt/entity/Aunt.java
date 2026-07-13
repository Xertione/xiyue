package com.xiyue.aunt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 阿姨资料实体。
 *
 * <p>与 {@code sys_user} 一对一关联（user_id 唯一），由阿姨自行注册时事务内创建。
 * 状态采用双字段分离设计（ADR-008）：
 * <ul>
 *   <li>{@link #adminStatus}：管理状态（管理员控制）AVAILABLE / OFF_SHELF / DISABLED</li>
 *   <li>{@link #acceptStatus}：接单状态（阿姨自控）AVAILABLE / RESTING</li>
 * </ul>
 * 档期占用由 aunt_booking_slot 表管理，不与以上字段混用。
 *
 * <p>本表在阶段 1（认证与注册）随阿姨注册事务一并创建记录；运营字段（标价、星级、
 * 技能标签等）的编辑/列表/详情接口在阶段 2「阿姨管理」实现。
 */
@Data
@TableName("aunt")
public class Aunt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 sys_user.id（一对一，唯一索引） */
    private Long userId;

    /** 姓名（注册后可补充） */
    private String name;

    /** 头像 URL */
    private String avatar;

    /** 标价（指定阿姨模式按此价计算） */
    private BigDecimal price;

    /** 星级（0.0 - 5.0） */
    private BigDecimal rating;

    /** 服务次数 */
    private Integer serviceCount;

    /** 年龄 */
    private Integer age;

    /** 入行年限 */
    private Integer experience;

    /** 技能标签（逗号分隔） */
    private String skillTags;

    /** 个人介绍 */
    private String intro;

    /** 管理状态：AVAILABLE / OFF_SHELF / DISABLED */
    private String adminStatus;

    /** 接单状态：AVAILABLE / RESTING */
    private String acceptStatus;

    /** 逻辑删除：0=未删 1=已删（MyBatis-Plus @TableLogic 自动处理） */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
