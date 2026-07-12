package com.xiyue.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiyue.aunt.entity.Aunt;
import com.xiyue.aunt.mapper.AuntMapper;
import com.xiyue.auth.dto.*;
import com.xiyue.common.enums.RoleEnum;
import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.ResultCode;
import com.xiyue.security.JwtUtil;
import com.xiyue.user.entity.SysUser;
import com.xiyue.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务：注册、登录、找回密码。
 *
 * <p>核心规则（见 04-agent-project-spec.md §7.1）：
 * <ul>
 *   <li>注册角色白名单：仅 USER / AUNT，ADMIN 禁止注册；</li>
 *   <li>密码使用 BCrypt 哈希存储；</li>
 *   <li>JWT 角色来自数据库，不信任客户端；</li>
 *   <li>阿姨注册在同一事务内创建 sys_user + aunt 资料（ADR-009）；</li>
 *   <li>手机号唯一性：预检 + 数据库唯一索引兜底（DuplicateKeyException 由全局处理器转换）。</li>
 * </ul>
 *
 * <p>日志安全：不输出密码 / 验证码 / 完整 Token；手机号仅记录后 4 位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final AuntMapper auntMapper;
    private final PasswordEncoder passwordEncoder;
    private final SmsCodeService smsCodeService;
    private final JwtUtil jwtUtil;

    /**
     * 注册。
     *
     * <p>事务内完成：校验验证码 → 角色白名单 → 手机号唯一预检 → 插入 sys_user
     * → 若为 AUNT 同时插入 aunt 资料。
     *
     * @param req 注册请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest req) {
        // 1. 角色白名单：仅 USER / AUNT，禁止 ADMIN（先做无副作用的校验，不消费验证码）
        if (!RoleEnum.isRegisterable(req.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅允许注册 USER 或 AUNT 角色，禁止注册 ADMIN");
        }

        // 2. 手机号唯一性预检（数据库唯一索引兜底，防并发注册；不消费验证码）
        Long existCount = sysUserMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, req.getPhone())
        );
        if (existCount != null && existCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该手机号已注册");
        }

        // 3. 校验验证码（校验通过会删除，一次性；放最后避免无效消费）
        smsCodeService.verifyCode(req.getPhone(), req.getCode());

        // 4. 插入 sys_user
        SysUser user = new SysUser();
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setNickname(req.getRole().toLowerCase() + "_" + maskPhone(req.getPhone()));
        sysUserMapper.insert(user);

        // 5. 阿姨角色：同事务创建 aunt 资料（ADR-009）
        if (RoleEnum.AUNT.name().equals(req.getRole())) {
            Aunt aunt = new Aunt();
            aunt.setUserId(user.getId());
            aunt.setAdminStatus("AVAILABLE");
            aunt.setAcceptStatus("AVAILABLE");
            auntMapper.insert(aunt);
        }

        log.info("注册成功（角色 {}，手机号尾号 {}）", req.getRole(), maskPhone(req.getPhone()));
    }

    /**
     * 密码登录。
     */
    public LoginResponse loginByPassword(String phone, String password) {
        SysUser user = getByPhone(phone);
        // 手机号不存在或密码不匹配，统一返回相同提示（防枚举手机号）
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "手机号或密码错误");
        }
        return buildLoginResponse(user);
    }

    /**
     * 验证码登录。
     */
    public LoginResponse loginByCode(String phone, String code) {
        // 先校验账号存在（不消费验证码），再校验验证码
        SysUser user = getByPhone(phone);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该手机号尚未注册");
        }
        smsCodeService.verifyCode(phone, code);
        return buildLoginResponse(user);
    }

    /**
     * 找回密码：校验验证码后重置密码。
     */
    public void resetPassword(String phone, String code, String newPassword) {
        // 先校验账号存在（不消费验证码），再校验验证码
        SysUser user = getByPhone(phone);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该手机号尚未注册");
        }
        smsCodeService.verifyCode(phone, code);
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        log.info("密码已重置（手机号尾号 {}）", maskPhone(phone));
    }

    /**
     * 获取当前登录用户信息。
     */
    public ProfileResponse getProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return ProfileResponse.builder()
            .userId(user.getId())
            .phone(user.getPhone())
            .role(user.getRole())
            .nickname(user.getNickname())
            .build();
    }

    // ===== 内部方法 =====

    private SysUser getByPhone(String phone) {
        return sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)
        );
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        // 角色取自数据库，不信任客户端
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), user.getRole());
        log.info("登录成功（角色 {}，手机号尾号 {}）", user.getRole(), maskPhone(user.getPhone()));
        return LoginResponse.builder()
            .token(token)
            .userId(user.getId())
            .phone(user.getPhone())
            .role(user.getRole())
            .nickname(user.getNickname())
            .build();
    }

    /** 手机号脱敏：仅保留后 4 位 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(phone.length() - 4);
    }
}
