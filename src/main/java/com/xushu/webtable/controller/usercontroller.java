package com.xushu.webtable.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xushu.webtable.anno.Log;
import com.xushu.webtable.anno.Role;
import com.xushu.webtable.common.*;
import com.xushu.webtable.service.userService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/Mydisk/user")
public class usercontroller {
    @Autowired
    private userService userservice;
    /**
     * 封禁用户
     */
    @Log(value = "封禁用户",operationType = Log.OperationType.BAN)
    @Operation(summary = "封禁用户",description = "根据用户id封禁用户，禁止用户登录")
    @Role(value = {Const.ROLE_ADMIN})
    @PostMapping("/admin/ban")
    public Result ban(@Parameter(description = "用户id") @RequestParam Integer userId,
                      @Parameter(description = "封禁等级,一共分为3个等级，1级：7天 2级：30天 3级：永久") int level,
                      @Parameter(description = "封禁理由") String reason){
        userservice.ban(userId,level,reason);
        return Result.success();
    }


    @Log(value = "解封用户",operationType = Log.OperationType.UNBAN)
    @Operation(summary = "解封用户",description = "根据用户id解封用户")
    @Role(value = {Const.ROLE_ADMIN})
    @PostMapping("/admin/unban")
    public Result unban(@Parameter(description = "用户id") @RequestParam Integer userId){
        userservice.unban(userId);
        return Result.success();
    }


    @Log(value="查询所有用户",operationType = Log.OperationType.QUERY)
    @Operation(summary = "查询所有用户",description = "查询所有用户")
    @Role(value={Const.ROLE_ADMIN})
    @PostMapping("/admin/selectUser")
    public Result selectlUser(
            @Parameter(description="用户名")@RequestParam(defaultValue = "")String userName,
            @Parameter(description = "页码")@RequestParam(defaultValue = Const.PAGE_DEFAULT+"") int page,
            @Parameter(description = "每页数量")@RequestParam(defaultValue = Const.SIZE_DEFAULT+"") int number
    ){
      return  Result.success(userservice.selectUserByName(userName,page, number));
    }


    @Log(value="查询所有被封禁的用户名单",operationType = Log.OperationType.QUERY)
    @Operation(summary = "查询所有被封禁的用户名单",description = "查询所有被封禁的用户名单")
    @Role(value={Const.ROLE_ADMIN})
    @PostMapping("/admin/selectBanned")
    public Result selectBanned(
            @Parameter(description="用户名")@RequestParam(defaultValue = "")String userName,
            @Parameter(description = "页码")@RequestParam(defaultValue = Const.PAGE_DEFAULT+"") int page,
            @Parameter(description = "每页数量")@RequestParam(defaultValue = Const.SIZE_DEFAULT+"") int number
    ){
        return Result.success(userservice.selectBannedByName(userName,page, number));
    }

}
