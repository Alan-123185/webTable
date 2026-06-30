package com.xushu.webtable.Tasks;

import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.OperationLog;
import com.xushu.webtable.service.logService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 死信队列处理导入失败的log日志
 */
@Component
@Slf4j
public class DeadLetterConsumer {

    // 注入专用的 RedisTemplate<String, OperationLog>
    @Autowired
    private RedisTemplate<String, OperationLog> operationLogRedisTemplate;

    @Autowired
    private logService logService;

    // 每分钟扫描一次死信队列（频率比正常队列低）
    @Scheduled(fixedDelay = 60000)
    public void processDeadLetters() {
        List<OperationLog> deadLogs = new ArrayList<>();
        for (int i = 0; i < Const.LOG_MAX_SIZE; i++) {
            // 使用正确的 RedisTemplate 获取操作日志
            OperationLog log = operationLogRedisTemplate.opsForList()
                    .leftPop(Const.REDIS_DEAD_LOG_ERROR_KEY_PREFIX);
            if (log == null) break;
            deadLogs.add(log);
        }
        if (deadLogs.isEmpty()) return;
        
        // 死信处理：写入本地文件或发送告警，或人工介入后重新入队
        for (int i = 0; i < 3; i++) {
            try {
                logService.insert(deadLogs);
                break;
            } catch (Exception e) {
                if(i==2){
                    log.error("批量插入日志失败，已放弃重试");
                    break;
                }
                log.error("第 {} 次重试失败", i + 1);
            }
        }
    }
}