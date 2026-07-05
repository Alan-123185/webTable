package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class selectUserResult {
    private List<selectUserBean> userList;
    private long total;
}
