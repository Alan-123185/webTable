package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private Integer code;      // 0:成功, 1:未注册, 2:密码错误
    private User user;         // 登录成功时才有值
}
