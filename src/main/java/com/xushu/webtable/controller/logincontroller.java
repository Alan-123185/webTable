package com.xushu.webtable.controller;

import com.xushu.webtable.common.LoginResult;
import com.xushu.webtable.common.Result;
import com.xushu.webtable.common.User;
import com.xushu.webtable.common.logininfo;
import com.xushu.webtable.service.loginService;
import com.xushu.webtable.utils.jwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/Mydisk")
public class logincontroller {
    @Autowired
    private loginService loginservice;
    @PostMapping("/login")
    public Result login(@RequestBody User user){
      LoginResult ret= loginservice.login(user);
       Integer res=ret.getCode();
       user=ret.getUser();
        if(res==0){
           log.info("用户{}登录成功！",user);
           String token = jwtUtils.makejwt(user.getUserName(), user.getId());
           logininfo info=new logininfo(user.getUserName(),user.getId(),token);
           return Result.success(info);
       }
       if(res==1){
           log.info("{}未注册！",user);
           return Result.error("用户未注册");
       }
       if(res==2){
           log.info("用户名或密码错误！{}",user);
           return Result.error("用户名或密码错误");
       }
       return Result.error("未知错误");
    }
}
