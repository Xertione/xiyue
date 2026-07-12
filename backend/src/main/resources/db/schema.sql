-- ============================================================
-- 息悦生活家政平台 - 数据库建表脚本（阶段 1-4）
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
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id) COMMENT '一个用户只能有一条阿姨资料'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='阿姨资料（个人信息+运营状态）';

-- ------------------------------------------------------------
-- service_order：预约订单（含地址快照、价格快照、阿姨快照、支付与退款字段）
-- 阶段 3 建表。订单状态机 9 态（ADR-003），金额 DECIMAL(10,2) + Java BigDecimal（规范 §6）。
-- 待支付/待抢单不占档期；抢单/指派成功才锁档期；取消释放档期（规范 §5.3、§7.6）。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no        VARCHAR(32)   NOT NULL COMMENT '订单号（全局唯一）',
    user_id         BIGINT        NOT NULL COMMENT '下单用户 sys_user.id',
    aunt_id         BIGINT        DEFAULT NULL COMMENT '抢单/指派阿姨 aunt.id（待支付/待抢单为 NULL）',
    service_date    DATE          NOT NULL COMMENT '服务日期',
    start_hour      TINYINT       NOT NULL COMMENT '开始小时（0-23 整点）',
    duration_hours  TINYINT       NOT NULL COMMENT '服务时长（小时，>=1）',
    contact_name    VARCHAR(50)   NOT NULL COMMENT '联系人姓名（快照）',
    contact_phone   VARCHAR(11)   NOT NULL COMMENT '联系电话（快照）',
    address         VARCHAR(255)  NOT NULL COMMENT '服务地址（快照）',
    amount          DECIMAL(10,2) NOT NULL COMMENT '订单金额（价格快照）',
    aunt_name       VARCHAR(50)   DEFAULT NULL COMMENT '阿姨姓名（抢单/指派时快照）',
    aunt_avatar     VARCHAR(255)  DEFAULT NULL COMMENT '阿姨头像（抢单/指派时快照）',
    status          INT           NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付 1待抢单 2待服务 3服务中 4待确认 5待评价 6已完成 7已取消 8投诉中',
    pay_no          VARCHAR(64)   DEFAULT NULL COMMENT '模拟支付流水号',
    pay_time        DATETIME      DEFAULT NULL COMMENT '支付时间',
    pay_method      VARCHAR(20)   DEFAULT NULL COMMENT '支付方式（MOCK）',
    refund_status   VARCHAR(20)   DEFAULT NULL COMMENT '退款状态：REFUNDED 模拟退款成功',
    refund_no       VARCHAR(64)   DEFAULT NULL COMMENT '模拟退款流水号',
    refund_time     DATETIME      DEFAULT NULL COMMENT '模拟退款时间',
    cancel_time     DATETIME      DEFAULT NULL COMMENT '取消时间',
    complete_image  VARCHAR(500)  DEFAULT NULL COMMENT '服务完成演示图片URL（阿姨提交完成时上传）',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no) COMMENT '订单号唯一索引',
    KEY idx_user_status (user_id, status, create_time) COMMENT '用户订单列表',
    KEY idx_aunt_status (aunt_id, status, service_date) COMMENT '阿姨订单列表',
    KEY idx_status_date (status, service_date) COMMENT '抢单大厅与管理员筛选'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='预约订单（含地址/价格/阿姨快照）';

-- ------------------------------------------------------------
-- aunt_booking_slot：阿姨小时块档期占用记录
-- 联合唯一索引 uk_aunt_date_hour 保障同一阿姨同一日期同一小时块只能被一单占用（ADR-002、ADR-010）。
-- 抢单/指派成功时插入；取消时删除释放。待支付/待抢单不插入。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS aunt_booking_slot (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    aunt_id      BIGINT   NOT NULL COMMENT '占用档期的阿姨 aunt.id',
    order_id     BIGINT   NOT NULL COMMENT '关联订单 service_order.id',
    service_date DATE     NOT NULL COMMENT '服务日期',
    hour_slot    TINYINT  NOT NULL COMMENT '小时块（0-23，整点）',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_aunt_date_hour (aunt_id, service_date, hour_slot) COMMENT '同一阿姨同一日期同一小时块唯一',
    KEY idx_order (order_id) COMMENT '按订单释放档期'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='阿姨小时块档期占用记录';

-- ------------------------------------------------------------
-- review：用户评价（阶段 4）
-- 一个订单只能评价一次（uk_review_order）。评价后订单变为已完成，更新阿姨评分与服务次数。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS review (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id    BIGINT      NOT NULL COMMENT '关联订单 service_order.id',
    user_id     BIGINT      NOT NULL COMMENT '评价用户 sys_user.id',
    rating      INT         NOT NULL COMMENT '评分（1-5）',
    content     TEXT        NOT NULL COMMENT '评价内容',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_order (order_id) COMMENT '一个订单只能评价一次'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户评价';

-- ------------------------------------------------------------
-- complaint：用户投诉（阶段 4，简化版）
-- 一个订单只能投诉一次（uk_complaint_order）。仅待评价订单可投诉；管理员处理后订单已完成，不再允许评价。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaint (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id      BIGINT      NOT NULL COMMENT '关联订单 service_order.id',
    user_id       BIGINT      NOT NULL COMMENT '投诉用户 sys_user.id',
    reason        TEXT        NOT NULL COMMENT '投诉原因',
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING 待处理 / HANDLED 已处理',
    handle_remark TEXT        DEFAULT NULL COMMENT '管理员处理备注',
    handle_time   DATETIME    DEFAULT NULL COMMENT '处理时间',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_complaint_order (order_id) COMMENT '一个订单只能投诉一次',
    KEY idx_status_create (status, create_time) COMMENT '管理员投诉列表按状态筛选+时间排序'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户投诉';

-- 已有数据库补索引（CREATE TABLE IF NOT EXISTS 不会为已存在的表加索引）：
-- ALTER TABLE complaint ADD INDEX idx_status_create (status, create_time);
