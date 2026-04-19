package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class shareinfo {
    private Integer id;
    private String linkName;
    private Integer fileId;
    private LocalDateTime loseTime;
    public shareinfo(String linkName, Integer fileId,LocalDateTime loseTime){
        this.linkName=linkName;
        this.fileId=fileId;
        this.loseTime=loseTime;
    }
}
