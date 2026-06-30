package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class File {
    private String fileName;        // 磁盘文件名（MD5）
    private Integer id;
    private String originalFileName; // 原始文件名
    private Integer userId;
    private String path;            // 完整路径
    private LocalDateTime createTime;
    private Long fileSize;
    private Integer refCount;       // 引用次数
    private Integer parentId;       // 父目录ID
    private Integer isFolder;
    // File.java 新增


    public File(String fileName,String originalFileName, Integer userId, Long fileSize, String path, LocalDateTime now,Integer parentId,Integer isFolder) {
        this.fileName=fileName;
        this.createTime=now;
        this.originalFileName=originalFileName;
        this.fileSize=fileSize;
        this.path=path;
        this.userId=userId;
        this.parentId=parentId;
        this.isFolder=isFolder;
    }
    public File(String fileName,String originalFileName, Integer userId, Long fileSize, String path, LocalDateTime now,Integer refCount,Integer parentId,Integer isFolder) {
        this.fileName=fileName;
        this.createTime=now;
        this.originalFileName=originalFileName;
        this.fileSize=fileSize;
        this.path=path;
        this.userId=userId;
        this.refCount=refCount;
        this.parentId=parentId;
        this.isFolder=isFolder;
    }
}
