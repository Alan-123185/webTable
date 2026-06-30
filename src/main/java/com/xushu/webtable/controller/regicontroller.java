package com.xushu.webtable.controller;

import com.xushu.webtable.anno.Log;
import com.xushu.webtable.anno.Role;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.RegisterRequest;
import com.xushu.webtable.common.Result;
import com.xushu.webtable.common.User;
import com.xushu.webtable.service.regiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/Mydisk")
@Tag(name = "用户注册")
public class regicontroller {
    @Autowired
    private regiService regiservice;
    @PostMapping("/getCode")
    @Operation(summary="获取验证码",description="发送注册请求获取验证码")
    @Role(value = {Const.ROLE_GUEST})
    public Result getCode(@Parameter(description = "包装好的注册请求类")@Valid @RequestBody RegisterRequest registerRequest){
        Boolean ret = regiservice.getCode(registerRequest);
        if(ret){
            //regiservice.makedir(user);
            return Result.success("验证码已发送");
        }
        return Result.error("注册失败");
    }
    @Log(value="用户注册",operationType = Log.OperationType.REGISTER)
    @Operation(summary="用户注册",description="校验验证码")
    @PostMapping("/register")
    @Role(value = {Const.ROLE_GUEST})
    public Result register(
            @Parameter(description = "包装好的用户类")@RequestBody RegisterRequest registerRequest,
            @Parameter(description = "验证码") @RequestParam String code){
        Integer ret = regiservice.Register(registerRequest,code);
       switch (ret){
           case Const.REGISTER_SUCCESS:
               return Result.success("注册成功");
           case Const.REGISTER_CODE_WRONG:
               return Result.error("验证码错误");
           case Const.REGISTER_CODE_EXPIRED:
               return Result.error("验证码已过期");
       }
        return Result.error("注册失败");
    }
}
