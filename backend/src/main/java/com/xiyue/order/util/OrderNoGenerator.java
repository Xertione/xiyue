package com.xiyue.order.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单号生成器。
 *
 * <p>格式：SO + yyyyMMddHHmmssSSS（17位）+ 3位随机数（100-999），共 22 位。
 *
 * <p>并发冲突由数据库唯一索引 uk_order_no 兜底，DuplicateKeyException 由
 * GlobalExceptionHandler 转为 1004 友好提示。同一毫秒内冲突概率 1/900，可接受。
 *
 * <p>时钟回拨保护：用 {@link #lastMillis} 记录上次时间戳，当前时间小于上次时
 * 取上次时间+1，保证时间戳单调递增，避免回拨导致重复订单号。
 *
 * <p>不使用 Redis 自增或雪花算法，保持 MVP 简单可读。
 */
@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String PREFIX = "SO";

    /** 上次生成的时间戳（毫秒），用于防时钟回拨，保证单调递增 */
    private final AtomicLong lastMillis = new AtomicLong(0L);

    /**
     * 生成订单号。
     */
    public String generate() {
        long now = System.currentTimeMillis();
        long effective = now;
        // CAS 循环：确保 effective >= lastMillis，防时钟回拨
        while (true) {
            long prev = lastMillis.get();
            if (now > prev) {
                effective = now;
            } else {
                // 时钟回拨或同毫秒，用上次时间+1 保证单调
                effective = prev + 1;
            }
            if (lastMillis.compareAndSet(prev, effective)) {
                break;
            }
            // CAS 失败说明并发已更新，重试
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(effective), ZoneId.systemDefault());
        return PREFIX + ldt.format(FMT) + random3();
    }

    private String random3() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }
}
