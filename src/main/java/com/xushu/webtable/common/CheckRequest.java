package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckRequest {
    private String md5;         // 整个文件的 MD5 指纹
    private String fileName;    // 用户原始文件名
    private long fileSize;      // 文件总大小（字节）
    private int totalChunks;    // 总分片数
    private String taskId;      //残留任务id，如果是全新任务则为null

}
