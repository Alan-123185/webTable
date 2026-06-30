package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RecycleFileInfo {
    private Integer id;
    private Integer fileId;
    private Integer userId;
    private LocalDateTime createTime;
    private Long fileSize;
    private String fileName;
    private String path;
    private Integer isFolder;
    private Integer parentId;
    private String originalFileName;
}
