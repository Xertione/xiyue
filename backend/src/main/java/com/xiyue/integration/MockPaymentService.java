package com.xiyue.integration;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟支付服务（规范 §7.4）。
 *
 * <p>第一版不接入真实微信支付，仅生成模拟支付/退款流水号，记录支付时间和方式。
 * 保留服务抽象，但不要虚构已实现微信支付能力。
 *
 * <p>流水号格式：PAY/RF + yyyyMMddHHmmssSSS + 3位随机数。
 */
@Service
public class MockPaymentService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 生成模拟支付流水号。
     */
    public String generatePayNo() {
        return "PAY" + LocalDateTime.now().format(FMT) + random3();
    }

    /**
     * 生成模拟退款流水号。
     */
    public String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(FMT) + random3();
    }

    private String random3() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }
}
