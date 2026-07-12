package com.xiyue.auth.service;

import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 短信验证码服务（模拟）。
 *
 * <p>第一版不接入真实短信，固定验证码为 {@code 123456}，缓存到 Redis：
 * <ul>
 *   <li>验证码 Key：{@code sms:code:{phone}}，Value：{@code 123456}，TTL 5 分钟</li>
 *   <li>频率限制 Key：{@code sms:code:limit:{phone}}，TTL 由 {@code xiyue.sms.resend-interval-seconds} 配置（默认 60 秒）</li>
 * </ul>
 *
 * <p>校验通过后立即删除验证码（一次性），防止验证码被重复使用。
 * 同一手机号在频率限制间隔内只能请求一次验证码，防止刷接口。
 *
 * <p>日志安全：不输出验证码值；手机号仅记录后 4 位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private final StringRedisTemplate redisTemplate;

    /** 验证码 Redis Key 前缀 */
    private static final String CODE_PREFIX = "sms:code:";

    /** 频率限制 Redis Key 前缀 */
    private static final String LIMIT_PREFIX = "sms:code:limit:";

    /** 固定验证码（第一版不接真实短信） */
    private static final String FIXED_CODE = "123456";

    /** 验证码有效期 */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    /** 频率限制间隔（秒），可配置 */
    @Value("${xiyue.sms.resend-interval-seconds:60}")
    private long resendIntervalSeconds;

    /**
     * 发送验证码：把固定验证码写入 Redis（模拟短信下发）。
     *
     * <p>同一手机号在 {@code resend-interval-seconds} 内只能请求一次，防止刷接口。
     *
     * @param phone 手机号
     * @throws BusinessException 发送太频繁
     */
    public void sendCode(String phone) {
        // 频率限制：同一手机号间隔内只能发一次
        String limitKey = LIMIT_PREFIX + phone;
        Boolean limitExists = redisTemplate.hasKey(limitKey);
        if (Boolean.TRUE.equals(limitExists)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "发送太频繁，请 " + resendIntervalSeconds + " 秒后再试");
        }

        String key = CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(key, FIXED_CODE, CODE_TTL);
        // 设置频率限制标记
        redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(resendIntervalSeconds));
        log.info("验证码已发送（手机号尾号 {}）", maskPhone(phone));
    }

    /**
     * 校验验证码：从 Redis 取出比对，匹配后删除（一次性）。
     *
     * @param phone      手机号
     * @param inputCode  用户输入的验证码
     * @throws BusinessException 验证码过期/未发送/不匹配
     */
    public void verifyCode(String phone, String inputCode) {
        String key = CODE_PREFIX + phone;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "验证码已过期或未发送，请重新获取");
        }
        if (!cached.equals(inputCode)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "验证码错误");
        }
        // 校验通过后删除，保证一次性
        redisTemplate.delete(key);
    }

    /** 手机号脱敏：仅保留后 4 位 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(phone.length() - 4);
    }
}
