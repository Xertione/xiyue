package com.xiyue.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * 登录用户主体（封装在 SecurityContext 中）。
 *
 * <p>由 {@link JwtAuthenticationFilter} 解析 JWT 后构造，包含用户 ID、手机号、角色。
 * 角色转换为 Spring Security 的 {@code ROLE_<role>} 权限，便于后续 {@code @PreAuthorize} 使用。
 *
 * <p>本类不存储密码，仅用于标识已认证身份。
 */
@Getter
@AllArgsConstructor
public class LoginUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String phone;
    private final String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
