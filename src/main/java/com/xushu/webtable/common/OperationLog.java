package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationLog {
    private Long id;
    private Integer operatorId;
    private String description;
    private String operateType;
    private Boolean status;
    private Long costMs;
    private LocalDateTime createTime;
}
