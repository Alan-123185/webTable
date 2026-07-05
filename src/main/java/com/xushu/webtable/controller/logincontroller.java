package com.xushu.webtable.controller;


import com.xushu.webtable.anno.Log;
import com.xushu.webtable.anno.Role;
import com.xushu.webtable.common.*;
import com.xushu.webtable.service.loginService;
import com.xushu.webtable.utils.CurrentHolder;
import com.xushu.webtable.utils.jwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/Mydisk")
@Tag(name = "登录管理")
public class logincontroller {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private loginService loginservice;
    @Log(value="用户登录",operationType = Log.OperationType.LOGIN)
    @Role(value ={Const.ROLE_GUEST})
    @Operation(summary = "用户登录",description = "用户登录接口，返回含有登录令牌的info类")
    @PostMapping("/login")
    public Result login(@Parameter(description = "包装好的登录请求类") @Valid @RequestBody LoginRequest loginRequest){
        //判断是否封号
       LoginResult ret= loginservice.loginpan(loginRequest);
       Integer res=ret.getCode();
       User user=ret.getUser();
       return loginservice.login(user,res);

    }


    @Log(value="退出登录",operationType = Log.OperationType.LOGOUT)
    @Role(value ={Const.ROLE_USER,Const.ROLE_ADMIN})
    @Operation(summary = "退出登录",description = "退出登录接口，清除token")
    @PostMapping("/logout")
    public Result logout(){
        return loginservice.logout();
    }

    @Log(value="获取最新用户最新存储空间",operationType = Log.OperationType.UPDATE)
    @Role(value ={Const.ROLE_USER,Const.ROLE_ADMIN})
    @Operation(summary = "实时获取用户最新存储空间",description = "获取用户的volume字段,同时更新redis把它放进去方便读取")
    @GetMapping("/updatevolume")
    public Result updatevolume(){
        Integer id=CurrentHolder.get();
        Long volume=loginservice.getVolume(id);
        return Result.success(volume);
    }


    @Log(value="从缓存里面获取用户的",operationType = Log.OperationType.OTHER)
    @Role(value ={Const.ROLE_USER,Const.ROLE_ADMIN})
    @Operation(summary = "从缓存里面获取用户的",description = "从缓存里面获取用户的volume")
    @GetMapping("/getvolume")
    public Result getvolume(){
        Integer id=CurrentHolder.get();
        Long volume=(Long) redisTemplate.opsForValue().get(Const.REDIS_USER_VOLUME+id);
        return Result.success(volume);
    }



}
