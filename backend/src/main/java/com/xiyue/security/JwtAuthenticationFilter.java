package com.xiyue.security;

import com.xiyue.user.entity.SysUser;
import com.xiyue.user.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>每次请求从 {@code Authorization: Bearer <token>} 头部提取 JWT：
 * <ul>
 *   <li>无 token / 非 Bearer → 直接放行，由 Security 后续决定 401 或放行；</li>
 *   <li>token 有效 → 解析 claims，并校验 pwdSig（密码哈希前 16 位）：
 *     查数据库当前密码哈希，与 token 中 pwdSig 比对，不匹配说明密码已更改，旧 token 失效；</li>
 *   <li>token 无效 / 过期 / 密码已改 → 清空 SecurityContext，由 Security 返回 401。</li>
 * </ul>
 *
 * <p>性能说明：每次受保护请求会查一次 sys_user。MVP 学习项目可接受；
 * 后续可用 Redis 缓存用户密码签名降低 DB 压力。
 *
 * <p>日志安全：不输出完整 token，仅记录解析/校验失败的异常消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int PWD_SIG_LENGTH = 16;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtUtil.parseClaims(token);
                Long userId = Long.valueOf(claims.getSubject());
                String phone = claims.get("phone", String.class);
                String role = claims.get("role", String.class);
                String pwdSig = claims.get("pwdSig", String.class);

                // 校验密码签名：改密码后旧 token 的 pwdSig 不匹配，立即失效
                if (!verifyPasswordSignature(userId, pwdSig)) {
                    log.warn("JWT 密码签名校验失败（userId={}），密码可能已更改", userId);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                LoginUser loginUser = new LoginUser(userId, phone, role);
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // token 无效或过期，清空上下文，由 EntryPoint 返回 401
                log.warn("JWT 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 校验 token 中的密码签名与数据库当前密码哈希是否一致。
     *
     * @return true 一致（token 有效）；false 不一致（密码已改，旧 token 失效）
     */
    private boolean verifyPasswordSignature(Long userId, String pwdSig) {
        if (pwdSig == null || pwdSig.length() < PWD_SIG_LENGTH) {
            return false;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getPassword() == null || user.getPassword().length() < PWD_SIG_LENGTH) {
            return false;
        }
        return user.getPassword().substring(0, PWD_SIG_LENGTH).equals(pwdSig);
    }
}
