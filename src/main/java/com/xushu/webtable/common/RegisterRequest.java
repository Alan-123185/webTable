package com.xushu.webtable.common;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用于处理和校验注册信息的请求参数
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min=1,max=20,message="用户名长度必须在1-20之间")
    private String userName;
    @NotBlank(message="密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需在8-20位之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,20}$",
    message="密码必须包含大小写字母、数字")
    private String password;
    @NotBlank(message="邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
