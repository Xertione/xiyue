package com.xiyue.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiyue.common.enums.RoleEnum;
import com.xiyue.user.entity.SysUser;
import com.xiyue.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 管理员账号初始化器。
 *
 * <p>应用启动时检查 sys_user 表是否存在 ADMIN 角色账号：
 * <ul>
 *   <li>不存在 → 使用环境变量 {@code ADMIN_INIT_PASSWORD} 经 BCrypt 加密后插入一条 ADMIN 账号；</li>
 *   <li>已存在 → 跳过，保证幂等。</li>
 * </ul>
 *
 * <p>设计取舍（ADR-013）：ADMIN 密码只通过环境变量注入，不写入 init.sql / 源码 / 日志 / Git；
 * 数据库仅保存 BCrypt 哈希。这比在 init.sql 中硬编码明文或固定哈希更符合 ADR-012。
 *
 * <p>日志安全：不输出密码；手机号仅记录后 4 位。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminAccountInitializer {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${xiyue.admin.init-password:}")
    private String adminInitPassword;

    @Value("${xiyue.admin.phone:13800000000}")
    private String adminPhone;

    @Bean
    public ApplicationRunner initAdminAccount() {
        return args -> {
            if (adminInitPassword == null || adminInitPassword.isBlank()) {
                log.warn("未配置 xiyue.admin.init-password，跳过 ADMIN 账号初始化");
                return;
            }

            Long existCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, RoleEnum.ADMIN.name())
            );
            if (existCount != null && existCount > 0) {
                log.info("ADMIN 账号已存在，跳过初始化");
                return;
            }

            String tail = adminPhone.length() >= 4 ? adminPhone.substring(adminPhone.length() - 4) : "****";

            // 检查手机号是否已被非 ADMIN 账号占用（避免唯一索引冲突导致启动失败）
            Long phoneExist = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, adminPhone)
            );
            if (phoneExist != null && phoneExist > 0) {
                log.error("管理员手机号尾号 {} 已被其他账号占用，无法初始化 ADMIN。请更换 ADMIN_PHONE 环境变量或清理该账号", tail);
                return;
            }

            SysUser admin = new SysUser();
            admin.setPhone(adminPhone);
            admin.setPassword(passwordEncoder.encode(adminInitPassword));
            admin.setRole(RoleEnum.ADMIN.name());
            admin.setNickname("系统管理员");
            try {
                sysUserMapper.insert(admin);
                log.info("ADMIN 账号初始化完成（手机号尾号 {}）", tail);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发情况下预检通过仍可能冲突，不阻止应用启动
                log.error("ADMIN 账号初始化失败：手机号尾号 {} 唯一索引冲突，请更换 ADMIN_PHONE 环境变量", tail);
            }
        };
    }
}
