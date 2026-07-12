package com.xiyue.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性（绑定 application.yml 中 xiyue.jwt.*）。
 *
 * <p>密钥 {@link #secret} 必须通过环境变量 {@code JWT_SECRET} 注入，
 * 禁止写入源码或 Git；本地开发由 application-local.yml 提供。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xiyue.jwt")
public class JwtProperties {

    /** JWT 签名密钥，至少 32 字节，密码学安全随机值（算法由密钥长度自动选择） */
    private String secret;

    /** Token 有效期（毫秒），默认 7 天 */
    private long expirationMs = 604800000L;
}
