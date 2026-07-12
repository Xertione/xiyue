package com.xiyue.config;

import com.xiyue.security.JwtAuthenticationFilter;
import com.xiyue.security.RestAccessDeniedHandler;
import com.xiyue.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（阶段 1：JWT + 权限收紧）。
 *
 * <p>策略：
 * <ul>
 *   <li>无状态（STATELESS），不创建 HttpSession；</li>
 *   <li>禁用 CSRF（纯 JWT 接口，无 cookie session）；</li>
 *   <li>放开健康检查、接口文档、认证公开接口；其余接口需认证；</li>
 *   <li>JwtAuthenticationFilter 置于 UsernamePasswordAuthenticationFilter 之前；</li>
 *   <li>未认证 → RestAuthenticationEntryPoint 返回 401 JSON；无权限 → RestAccessDeniedHandler 返回 403 JSON。</li>
 * </ul>
 *
 * <p>角色细粒度控制（@PreAuthorize）留待阶段 2+ 各业务接口按需启用。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    // 健康检查
                    "/api/health/**",
                    // 接口文档
                    "/doc.html",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    // 认证公开接口
                    "/api/auth/register",
                    "/api/auth/login/**",
                    "/api/auth/reset-password",
                    "/api/auth/sms-code"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
