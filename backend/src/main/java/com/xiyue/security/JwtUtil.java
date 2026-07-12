package com.xiyue.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析。
 *
 * <p>算法 HS256（对称密钥）。Token claims：
 * <ul>
 *   <li>sub   用户 ID（字符串形式）</li>
 *   <li>phone 手机号</li>
 *   <li>role  角色（来自数据库，不信任客户端）</li>
 *   <li>iat   签发时间</li>
 *   <li>exp   过期时间</li>
 * </ul>
 *
 * <p>安全约束（ADR-012）：
 * <ul>
 *   <li>密钥至少 32 字节，启动时校验，不达标直接抛异常阻止启动；</li>
 *   <li>密钥不输出到日志；</li>
 *   <li>日志不输出完整 Token。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey key;

    @PostConstruct
    void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 密钥未配置（xiyue.jwt.secret / 环境变量 JWT_SECRET）");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT 密钥长度不足 32 字节，当前 " + bytes.length + " 字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        log.info("JWT 密钥已加载（长度 {} 字节）", bytes.length);
    }

    /**
     * 签发 JWT。
     *
     * <p>claims 中包含 pwdSig（密码哈希前 16 位），用于改密码后使旧 token 失效：
     * 改密码后 BCrypt 重新生成 salt，password 哈希变化，旧 token 的 pwdSig 不再匹配，
     * {@link JwtAuthenticationFilter} 校验失败，旧 token 立即失效。
     *
     * @param userId       用户 ID
     * @param phone        手机号
     * @param role         角色（来自数据库）
     * @param passwordHash BCrypt 密码哈希（取前 16 位作为签名）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String phone, String role, String passwordHash) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpirationMs());
        String pwdSig = (passwordHash != null && passwordHash.length() >= 16)
            ? passwordHash.substring(0, 16) : "";
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("phone", phone)
            .claim("role", role)
            .claim("pwdSig", pwdSig)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact();
    }

    /**
     * 解析并验证 JWT，返回 claims。
     *
     * @param token JWT 字符串
     * @return claims
     * @throws io.jsonwebtoken.JwtException token 无效或过期时抛出
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
