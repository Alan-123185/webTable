package com.xushu.webtable.Tasks;

import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.OperationLog;
import com.xushu.webtable.service.logService;
import com.xushu.webtable.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LogSaver {
    @Autowired
    private logService logservice;
    
    // 使用专门用于 OperationLog 的 RedisTemplate
    @Autowired
    private RedisTemplate<String, OperationLog> operationLogRedisTemplate;

    @Scheduled(fixedDelay = 5000)
    public void saveLogs() {
        List<OperationLog> logs = new ArrayList<>();
        for (int i = 0; i < Const.LOG_MAX_SIZE; i++) {
            // 使用正确的 RedisTemplate 获取 OperationLog 对象
            OperationLog log = operationLogRedisTemplate.opsForList().leftPop(Const.REDIS_LOG_INFO_KEY_PREFIX);
            if (log == null) break;
            if(log.getOperatorId()==null)
                log.setOperatorId(CurrentHolder.get());
            logs.add(log);
        }
        if (logs.isEmpty()) return;
        try {
            logservice.insert(logs);
            log.info("成功消费 {} 条操作日志", logs.size());
        } catch (Exception e) {
            log.error("批量插入日志失败，已将 {} 条日志转入死信队列", logs.size(), e);
            handleDeadLetter(logs);
        }
    }

    private void handleDeadLetter(List<OperationLog> logs) {
        for (OperationLog operationLog : logs) {
            // 使用正确的 RedisTemplate 存储 OperationLog 对象
            operationLogRedisTemplate.opsForList().rightPush(Const.REDIS_DEAD_LOG_ERROR_KEY_PREFIX, operationLog);
        }
    }
}