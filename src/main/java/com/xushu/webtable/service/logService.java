package com.xushu.webtable.service;

import com.xushu.webtable.common.OperationLog;

import java.util.List;

public interface logService {
    void insert(List<OperationLog> logs);
    void deleteLogs();
}
