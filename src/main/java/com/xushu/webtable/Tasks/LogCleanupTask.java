package com.xushu.webtable.Tasks;

import com.xushu.webtable.service.logService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogCleanupTask {
    @Autowired
    private logService logService;
    /**
     * 定时任务，每天0点执行一次
     * 清理日志
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24)
    public void cleanup() {
        logService.deleteLogs();
    }
}
