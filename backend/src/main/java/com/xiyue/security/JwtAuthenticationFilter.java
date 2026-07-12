package com.xiyue.security;

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
 *   <li>token 有效 → 解析 claims 构造 {@link LoginUser} 放入 SecurityContext；</li>
 *   <li>token 无效 / 过期 → 清空 SecurityContext，由 Security 返回 401。</li>
 * </ul>
 *
 * <p>日志安全：不输出完整 token，仅记录解析失败的异常消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String BEARER_PREFIX = "Bearer ";

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
}
