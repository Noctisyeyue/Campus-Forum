package com.campus.forum.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 限流通用工具，基于 Redis 实现多种限流策略
 */
@Slf4j
@Component
public class FlowUtils {

    @Resource
    StringRedisTemplate template;

    // 默认限流策略：超频时不额外处理
    private static final LimitAction defaultAction = overclock -> !overclock;

    /**
     * 单次频率限制，请求成功后在冷却时间内不得再次请求
     */
    public boolean limitOnceCheck(String key, int blockTime) {
        return this.internalCheck(key, 1, blockTime, defaultAction);
    }

    /**
     * 单次频率限制（带升级封禁），超频后封禁更长时间
     */
    public boolean limitOnceUpgradeCheck(String key, int frequency, int baseTime, int upgradeTime) {
        return this.internalCheck(key, frequency, baseTime, (overclock) -> {
            if (overclock)
                template.opsForValue().set(key, "1", upgradeTime, TimeUnit.SECONDS);
            return false;
        });
    }

    /**
     * 时间段内多次请求限制（带封禁），超频后封禁一段时间
     */
    public boolean limitPeriodCheck(String counterKey, String blockKey, int blockTime, int frequency, int period) {
        return this.internalCheck(counterKey, frequency, period, (overclock) -> {
            if (overclock)
                template.opsForValue().set(blockKey, "", blockTime, TimeUnit.SECONDS);
            return !overclock;
        });
    }

    /**
     * 时间段内多次请求限制（仅计数不封禁）
     */
    public boolean limitPeriodCounterCheck(String counterKey, int frequency, int period) {
        return this.internalCheck(counterKey, frequency, period, defaultAction);
    }

    // 限流核心逻辑，利用 Redis INCR 原子计数
    private boolean internalCheck(String key, int frequency, int period, LimitAction action) {
        if (Boolean.TRUE.equals(template.hasKey(key))) {
            Long value = Optional.ofNullable(template.opsForValue().increment(key)).orElse(0L);
            return action.run(value > frequency);
        } else {
            template.opsForValue().set(key, "1", period, TimeUnit.SECONDS);
            return true;
        }
    }

    // 限流策略函数式接口
    private interface LimitAction {
        boolean run(boolean overclock);
    }
}
