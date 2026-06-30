package com.xushu.webtable.controller;

import com.xushu.webtable.anno.Log;
import com.xushu.webtable.anno.Role;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.File;
import com.xushu.webtable.common.Result;
import com.xushu.webtable.service.recycleBinService;
import com.xushu.webtable.utils.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/Mydisk/recycleBin")
public class recycleBincontroller {
    @Autowired
    private recycleBinService recycleBinService;

    @Log(value = "查询回收站文件",operationType = Log.OperationType.QUERY)
    @Operation(summary = "查询回收站文件",description = "查询当前用户回收站所有文件")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @PostMapping("/selectall")
    public Result selectall(){
        List<File> files= recycleBinService.getRecycleFiles(CurrentHolder.get());
        return Result.success(files);
    }


    @Log(value = "回收文件",operationType = Log.OperationType.RESTORE)
    @Operation(summary = "文件恢复",description = "根据文件id批量恢复文件")
    @Role(value= {Const.ROLE_ADMIN,Const.ROLE_USER})
    @PostMapping("/goback")
    public Result goback(@Parameter(description = "文件id列表") @RequestParam List<Integer> fileIds){
        recycleBinService.goback(fileIds);
        return Result.success(fileIds);
    }

    @Log(value = "彻底删除文件",operationType = Log.OperationType.DELETE)
    @Operation(summary = "彻底删除文件",description = "根据文件id批量彻底删除文件")
    @Role(value= {Const.ROLE_ADMIN,Const.ROLE_USER})
    @PostMapping("/delete")
    public Result delete(@Parameter(description = "文件id列表") @RequestParam List<Integer> fileIds) {
        recycleBinService.delete(fileIds);
        return Result.success(fileIds);
    }

}
