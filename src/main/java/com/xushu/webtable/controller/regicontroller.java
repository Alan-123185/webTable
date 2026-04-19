package com.xushu.webtable.controller;

import com.xushu.webtable.common.Result;
import com.xushu.webtable.common.User;
import com.xushu.webtable.service.regiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/Mydisk")
public class regicontroller {
    @Autowired
    private regiService regiservice;
    @PostMapping("/register")
    public Result register(@RequestBody User user){
        Boolean ret = regiservice.register(user);
        if(ret){
            log.info("{}成功注册",user);
            //regiservice.makedir(user);
            return Result.success(user);
        }
        else return Result.error("注册失败,名字重复！");
    }
}
