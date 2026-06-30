package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class logininfo {
    private String username;
    private Long  id;
    private String token;
    private Long volume;
    private Long totalVolume;
}
