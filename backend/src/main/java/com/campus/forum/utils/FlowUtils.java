package com.campus.forum.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 限流通用工具，基于 Redis 实现多种限流策略
 * <p>
 * 核心思路：用 Redis 的 INCR 命令做原子计数，在指定时间窗口内统计请求次数，
 * 超过阈值则触发封禁。不同方法提供不同的超频处理策略。
 */
@Slf4j
@Component
public class FlowUtils {

    /** Redis 操作模板 */
    @Resource
    StringRedisTemplate template;

    /** 默认限流策略：超频时不额外处理，直接返回是否超频 */
    private static final LimitAction defaultAction = overclock -> !overclock;

    /**
     * 单次频率限制，请求成功后在冷却时间内不得再次请求
     *
     * @param key       Redis 计数器 key
     * @param blockTime 冷却时间（秒）
     * @return true=允许请求，false=冷却期内拒绝
     */
    public boolean limitOnceCheck(String key, int blockTime) {
        return this.internalCheck(key, 1, blockTime, defaultAction);
    }

    /**
     * 单次频率限制（带升级封禁），超频后将封禁时间延长
     * <p>
     * 用途：JWT 生成频率限制，正常请求有基础冷却，超频后封禁更长时间
     *
     * @param key         Redis 计数器 key
     * @param frequency   允许的请求次数阈值
     * @param baseTime    基础冷却时间（秒）
     * @param upgradeTime 超频后升级封禁时间（秒）
     * @return true=允许请求，false=冷却期内或超频拒绝
     */
    public boolean limitOnceUpgradeCheck(String key, int frequency, int baseTime, int upgradeTime) {
        return this.internalCheck(key, frequency, baseTime, (overclock) -> {
            if (overclock)
                template.opsForValue().set(key, "1", upgradeTime, TimeUnit.SECONDS);
            return false;
        });
    }

    /**
     * 时间段内多次请求限制（带封禁），超频后用单独的 key 标记封禁
     * <p>
     * 用途：验证码发送限制，计数器和封禁标记分开管理
     *
     * @param counterKey 计数器 key
     * @param blockKey   封禁标记 key
     * @param blockTime  封禁时间（秒）
     * @param frequency  允许的请求次数阈值
     * @param period     计数时间窗口（秒）
     * @return true=允许请求，false=超频拒绝
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
     *
     * @param counterKey 计数器 key
     * @param frequency  允许的请求次数阈值
     * @param period     计数时间窗口（秒）
     * @return true=未超频允许，false=超频拒绝
     */
    public boolean limitPeriodCounterCheck(String counterKey, int frequency, int period) {
        return this.internalCheck(counterKey, frequency, period, defaultAction);
    }

    /**
     * 限流核心逻辑，利用 Redis INCR 原子计数
     * <p>
     * 首次请求：在 Redis 创建计数器，值为 1，period 秒后过期 → 返回 true
     * 冷却期内：计数器 +1，超过阈值则通过 action 执行封禁策略 → 返回 action 的结果
     *
     * @param key       Redis 计数器 key
     * @param frequency 允许的请求次数阈值
     * @param period    计数器过期时间（秒）
     * @param action    超频处理策略，接收是否超频的布尔值，返回是否允许
     * @return true=允许请求，false=拒绝
     */
    private boolean internalCheck(String key, int frequency, int period, LimitAction action) {
        // 检查 Redis 里有没有这个 key
        if (Boolean.TRUE.equals(template.hasKey(key))) {
            // 有 key → 计数器 +1，检查是否超频   Optional.ofNullable 防止 null 导致程序崩溃
            Long value = Optional.ofNullable(template.opsForValue().increment(key)).orElse(0L);
            // 次数是否超过了阈值
            return action.run(value > frequency);
        } else {
            // 没有 key → 创建计数器，允许通过
            template.opsForValue().set(key, "1", period, TimeUnit.SECONDS);
            return true;
        }
    }

    /**
     * 限流策略函数式接口，定义超频后的处理方式
     * <p>
     * 不同调用方通过 Lambda 表达式传入不同的封禁策略，
     * 避免为每种策略写重复的计数逻辑
     */
    private interface LimitAction {
        /**
         * 执行限流策略
         *
         * @param overclock true=超频了，false=未超频
         * @return true=允许请求，false=拒绝
         */
        boolean run(boolean overclock);
    }
}
