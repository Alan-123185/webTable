package com.xushu.webtable.common;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * 不要直接使用User实体类接收请求，新建一个类专门用来接受，同时做好检验！
 */
@Data
public class LoginRequest implements Serializable {
    @NotBlank(message = "用户名不能为空")
    @Size(min=1,max=20,message="用户名长度必须在1-20之间")
    private String userName;
    @NotBlank(message="密码不能为空")
    @Size(min = 1, max = 20, message = "密码长度需在8-20位之间")
   @Pattern(
           regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "密码必须包含大小写字母、数字")
    private String password;
}
