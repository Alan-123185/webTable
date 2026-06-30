package com.xushu.webtable.service.maker;

import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.OperationLog;
import com.xushu.webtable.mapper.LogMapper;
import com.xushu.webtable.service.logService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException; // ← 必须导入这个类
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志服务实现类
 * 专门用来把日志记录存进数据库
 */
@Slf4j
@Service
public class logServicemake implements logService {

    @Autowired
    private LogMapper logMapper;

    /**
     * 插入操作日志到数据库
     * 调用方（LogSaver）已在 @Scheduled 异步线程中运行
     *
     * @param logs 操作日志对象
     * @throws ManageException 当遇到可重试的数据库异常时抛出（带原始异常）
     */
    @Override
    public void insert(List<OperationLog> logs) {
        try {
            logMapper.batchInsert(logs);
        } catch (DataAccessException e) {
            // 数据库连接类异常（可重试）
            log.error("操作日志保存失败（可重试），详情: {}",  e.getMessage());
            throw new ManageException(Const.HTTP_INTERNAL_ERROR,"操作日志保存失败（可重试）");
        } catch (Exception e) {
            // 其他异常（SQL语法错误等，永久错误）
            log.error("操作日志保存失败（永久错误）", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteLogs() {
        logMapper.deleteLogs(LocalDateTime.now().minusDays(30));
    }
}
