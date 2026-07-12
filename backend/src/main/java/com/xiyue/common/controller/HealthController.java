package com.xiyue.common.controller;

import com.xiyue.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，真实探测 MySQL 与 Redis 连通性。
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "健康检查", description = "服务健康检查接口")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping
    @Operation(summary = "健康检查", description = "探测 MySQL 与 Redis 连通性，返回各组件状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("mysql", checkMysql());
        detail.put("redis", checkRedis());
        boolean allUp = "up".equals(detail.get("mysql")) && "up".equals(detail.get("redis"));
        detail.put("status", allUp ? "up" : "down");
        return Result.success(detail);
    }

    private String checkMysql() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1 ? "up" : "down";
        } catch (Exception e) {
            log.warn("MySQL 健康检查失败: {}", e.getMessage());
            return "down";
        }
    }

    private String checkRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong) ? "up" : "down";
        } catch (Exception e) {
            log.warn("Redis 健康检查失败: {}", e.getMessage());
            return "down";
        }
    }
}
