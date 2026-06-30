package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckResult {
    private String md5;
    private String origfileName;
    private int totalChunks;
    private boolean exist;          // 是否秒传
    private Long fileSize;          // 文件大小
    private String taskId;        // 非秒传时返回的任务ID
    private List<Integer> uploadedChunks; // 已上传分片序号列表
}
