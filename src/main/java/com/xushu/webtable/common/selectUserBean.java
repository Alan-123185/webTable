package com.xushu.webtable.common;

import lombok.Data;

/**
 * id,status,volume,user_name,email,all_volume
 */
@Data
public class selectUserBean {
    private Integer id;
    private int status;
    private String email;
    private String userName;
    private Long volume;
    private Long allVolume;
}
