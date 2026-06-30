package com.xushu.webtable.mapper;

import com.xushu.webtable.common.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogMapper {
    /**
     * 插入操作日志
     *
     * @param logs 操作日志对象
     * @return 插入的行数
     */
    int batchInsert(List<OperationLog> logs);

    void deleteLogs(LocalDateTime time);
}
