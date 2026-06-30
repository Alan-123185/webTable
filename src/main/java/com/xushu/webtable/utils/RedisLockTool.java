package com.xushu.webtable.utils;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Redis 分布式锁工具类
 */
// 标记为Spring组件，交由Spring容器管理
@Component
public class RedisLockTool {

    // 自动注入Redis模板，用于操作Redis数据
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 看门狗线程池：为每个锁分配一个独立的续期任务，核心线程数为10
    private final ScheduledExecutorService watchdogExecutor = Executors.newScheduledThreadPool(10);
    // 记录每个 key 的续期 Future 对象，用于在解锁时取消对应的续期任务
    private final ConcurrentHashMap<String, ScheduledFuture<?>> renewalTasks = new ConcurrentHashMap<>();

    /**
     * 尝试加锁（带自动续期）
     * @param key          锁 key
     * @param leaseTime    初始租约时间（秒），续期后会保持这个长度
     * @param retryTimes   重试次数
     * @param retryIntervalMs 重试间隔
     * @return 锁标识（解锁时需要），失败返回 null
     */
    public String tryLock(String key, long leaseTime, int retryTimes, long retryIntervalMs) {
        // 生成唯一的锁标识值，确保锁的唯一性
        String lockValue = UUID.randomUUID().toString();
        // 循环尝试获取锁，直到达到最大重试次数
        for (int i = 0; i <= retryTimes; i++) {
            // 尝试设置键值对，如果键不存在则设置成功并返回true，同时设置过期时间
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, lockValue, Duration.ofSeconds(leaseTime));
            // 如果加锁成功
            if (Boolean.TRUE.equals(success)) {
                // 启动看门狗机制，定期续期以防止锁过期
                startWatchdog(key, lockValue, leaseTime);
                // 返回锁标识，供后续解锁使用
                return lockValue;
            }
            // 如果还未达到最大重试次数
            if (i < retryTimes) {
                // 等待指定间隔后重试，避免频繁请求 Redis
                try { Thread.sleep(retryIntervalMs); }
                // 若线程在等待期间被中断，恢复中断状态并退出重试循环
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
        // 重试次数用尽仍未获取到锁，返回null表示失败
        return null;
    }

    /**
     * 启动续期任务（每 leaseTime/3 秒执行一次）
     *
     */
    private void startWatchdog(String key, String lockValue, long leaseTime) {
        // 计算续期间隔，通常为租约时间的三分之一，以保证锁不会过期
        long period = leaseTime / 3; // 例如 30 秒续期一次
        // 防止间隔时间过小或为零，设置最小默认值为5秒
        if (period <= 0) period = 5;
        // 调度固定速率的任务，定期执行续期逻辑
        ScheduledFuture<?> future = watchdogExecutor.scheduleAtFixedRate(() -> {
            // 创建Lua脚本对象，加载名为getmoretime.lua的脚本，期望返回Long类型
            DefaultRedisScript<Long> script = new DefaultRedisScript<>("getmoretime.lua", Long.class);
            // 执行Lua脚本，传入key、锁标识和新的过期时间，实现原子性续期
            Long result = redisTemplate.execute(script, Collections.singletonList(key), lockValue, String.valueOf(leaseTime));
            // 如果脚本返回null或0，说明锁已失效或被其他线程占用
            if (result == null || result == 0) {
                // 锁已经被释放或被人占用，取消当前的续期任务
                cancelWatchdog(key);
            }
        }, period, period, TimeUnit.SECONDS); // 初始延迟period秒，之后每隔period秒执行一次
        // 将生成的Future对象存入地图，以便后续可以通过key取消任务
        renewalTasks.put(key, future);
    }

    /**
     * 取消续期任务
     */
    private void cancelWatchdog(String key) {
        // 从地图中移除并获取对应key的Future对象
        ScheduledFuture<?> future = renewalTasks.remove(key);
        // 如果Future对象存在
        if (future != null) {
            // 取消定时任务，false表示不中断正在执行的任务
            future.cancel(false);
        }
    }

    /**
     * 释放锁（自动取消续期）
     * 先取消看门狗
     * 再调用解锁方法
     */
    public boolean unlock(String key, String lockValue) {
        // 首先取消该key对应的看门狗续期任务
        cancelWatchdog(key);
        // 创建Lua脚本对象，加载名为unlock.lua的脚本，用于原子性释放锁
        DefaultRedisScript<Long> script = new DefaultRedisScript<>("unlock.lua", Long.class);
        // 执行解锁脚本，传入key和锁标识，确保只有持有锁的线程才能解锁
        Long result = redisTemplate.execute(script, Collections.singletonList(key), lockValue);
        // 如果返回结果为1，表示解锁成功，否则失败
        return result != null && result == 1L;
    }

    // Spring容器销毁前调用的方法，用于优雅关闭线程池
    @PreDestroy
    public void shutdown() {
        // 启动线程池的关闭过程，不再接受新任务
        watchdogExecutor.shutdown();
        try {
            // 等待最多5秒让已提交的任务执行完毕
            if (!watchdogExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                // 如果超时仍未终止，强制关闭线程池
                watchdogExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // 如果等待过程中被中断，强制关闭线程池
            watchdogExecutor.shutdownNow();
            // 恢复当前线程的中断状态
            Thread.currentThread().interrupt();
        }
    }
}