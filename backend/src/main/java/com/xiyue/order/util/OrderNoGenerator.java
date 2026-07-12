package com.xiyue.order.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成器。
 *
 * <p>格式：SO + yyyyMMddHHmmssSSS（17位）+ 3位随机数（100-999），共 22 位。
 *
 * <p>并发冲突由数据库唯一索引 uk_order_no 兜底，DuplicateKeyException 由
 * GlobalExceptionHandler 转为 1004 友好提示。同一毫秒内冲突概率 1/900，可接受。
 *
 * <p>不使用 Redis 自增或雪花算法，保持 MVP 简单可读。
 */
@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String PREFIX = "SO";

    /**
     * 生成订单号。
     */
    public String generate() {
        return PREFIX + LocalDateTime.now().format(FMT) + random3();
    }

    private String random3() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }
}
