-- ============================================================
-- 息悦生活家政平台 - 数据库建表脚本（阶段 1）
-- 幂等：全部使用 CREATE TABLE IF NOT EXISTS，可重复执行。
-- 由 Spring Boot spring.sql.init 在应用启动时自动执行。
-- ============================================================

-- ------------------------------------------------------------
-- sys_user：用户 / 阿姨 / 管理员账号
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    phone       VARCHAR(11)  NOT NULL COMMENT '手机号（11位，全局唯一）',
    password    VARCHAR(72)  NOT NULL COMMENT 'BCrypt 密码哈希',
    role        VARCHAR(10)  NOT NULL COMMENT '角色：USER / AUNT / ADMIN',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone) COMMENT '手机号唯一索引'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户/阿姨/管理员账号';

-- ------------------------------------------------------------
-- aunt：阿姨资料（与 sys_user 一对一）
-- 阶段 1 随阿姨注册事务一并创建记录；运营字段编辑/列表/详情在阶段 2 实现。
-- 状态双字段分离（ADR-008）：admin_status（管理员控制）+ accept_status（阿姨自控）。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS aunt (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT        NOT NULL COMMENT '关联 sys_user.id（一对一）',
    name          VARCHAR(50)   DEFAULT NULL COMMENT '姓名（注册后可补充）',
    avatar        VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
    price         DECIMAL(10,2) DEFAULT NULL COMMENT '标价（指定阿姨模式按此价计算）',
    rating        DECIMAL(2,1)  NOT NULL DEFAULT 0.0 COMMENT '星级（0.0 - 5.0）',
    service_count INT           NOT NULL DEFAULT 0 COMMENT '服务次数',
    skill_tags    VARCHAR(255)  DEFAULT NULL COMMENT '技能标签（逗号分隔）',
    intro         TEXT          DEFAULT NULL COMMENT '个人介绍',
    admin_status  VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE' COMMENT '管理状态：AVAILABLE / OFF_SHELF / DISABLED',
    accept_status VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE' COMMENT '接单状态：AVAILABLE / RESTING',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id) COMMENT '一个用户只能有一条阿姨资料'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='阿姨资料（个人信息+运营状态）';
