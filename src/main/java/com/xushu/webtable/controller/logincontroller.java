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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
       LoginResult ret= loginservice.login(loginRequest);
       Integer res=ret.getCode();
       User user=ret.getUser();
        if(res== Const.LOGIN_SUCCESS){
           log.info("用户{}登录成功！",user);
           String token = jwtUtils.makejwt(user.getUserName(), user.getId(), user.getRole());
           CurrentHolder.set(user.getId().intValue());
           logininfo info=new logininfo(user.getUserName(),user.getId(),token,user.getVolume(),user.getAllVolume());
           redisTemplate.opsForValue().set(Const.REDIS_LOGIN_INFO_KEY_PREFIX+user.getId(),
                   "1",
                   Duration.ofHours(Const.REDIS_TOKEN_LOSE_HOURS));
           return Result.success(info);
       }
       if(res== Const.LOGIN_NOT_REGISTERED){
           log.info("{}未注册！",user);
           return Result.error("用户未注册");
       }
       if(res== Const.LOGIN_PASSWORD_WRONG){
           log.info("用户名或密码错误！{}",user);
           return Result.error("用户名或密码错误");
       }
       return Result.error("未知错误");
    }

    @Log(value="退出登录",operationType = Log.OperationType.LOGOUT)
    @Role(value ={Const.ROLE_USER,Const.ROLE_ADMIN})
    @Operation(summary = "退出登录",description = "退出登录接口，清除token")
    @PostMapping("/logout")
    public Result logout(){
        redisTemplate.delete(Const.REDIS_LOGIN_INFO_KEY_PREFIX+ CurrentHolder.get());
        return Result.success();
    }
}
