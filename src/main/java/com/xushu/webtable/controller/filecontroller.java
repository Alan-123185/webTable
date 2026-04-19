package com.xushu.webtable.controller;

import com.xushu.webtable.common.File;
import com.xushu.webtable.common.Result;
import com.xushu.webtable.common.selectFileBean;
import com.xushu.webtable.common.shareinfo;
import com.xushu.webtable.service.fileService;
import com.xushu.webtable.utils.CurrentHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.xushu.webtable.common.Result.success;

@Slf4j
@RestController
@RequestMapping("/Mydisk")
public class filecontroller {
    @Autowired
    private fileService fileservice;
    @GetMapping("/select")
    public Result select(@RequestParam(defaultValue = "") String originalFileName,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int number,
                         @RequestParam(defaultValue = "0") int parentId){
        selectFileBean files = fileservice.select(originalFileName, CurrentHolder.get(), page, number,parentId);
        log.info("{}查询",originalFileName);
        return success(files);
    }
    @DeleteMapping("/delete")
    public Result delete(@RequestBody List<Integer> ids) throws IOException {
        fileservice.delete(ids);
        return Result.success();
    }
    @PostMapping("/share")
    public Result share(@RequestParam Integer fileId){
        shareinfo share = fileservice.share(fileId);
        return Result.success(share);
    }
    @GetMapping("/store/{link}")
    public Result store(@PathVariable String link,
                        @RequestParam(defaultValue = "0") Integer parentId,
                        HttpServletResponse response){
       // shareinfo linkinfo = fileservice.findlink(link);
        Integer panlink = fileservice.store(link,parentId);
        if(panlink==-2)
        {
            log.info("链接错误");
            response.setStatus(404);
            return Result.error("链接错误或者文件已被删除");
        }
        if(panlink==1)
        {
            log.info("链接失效");
            response.setStatus(401);
            return Result.error("链接失效");
        }
        if(panlink==-1){
            log.info("文件已存在");
            return Result.error("文件已存在");
        }
        if(panlink==-3){
            log.info("剩余空间不足");
            return Result.error("剩余空间不足");
        }
        log.info("成功转存");
        return Result.success();
    }
    @GetMapping("/download/")
    public void download(@RequestParam Integer fileId,
                         HttpServletResponse response,
                         @RequestParam(value = "parentId", required = false, defaultValue = "0") Integer parentId){
        try {
            fileservice.download(fileId, response);
            log.info("下载成功");
        } catch (IOException e) {
            e.printStackTrace();
            log.error("下载失败:{}",e.getMessage());
        }
    }
    @GetMapping("/preview/{fileId}")
    public void preview(@PathVariable Integer fileId, HttpServletResponse response){
        fileservice.preview(fileId, response);
        log.info("预览");
    }
    @PostMapping("/mkdir")
    public Result mkdir(@RequestParam String fileName,
                        @RequestParam(defaultValue = "0") Integer parentId){
        fileservice.mkdir(fileName,parentId);
        return Result.success(fileName);
    }
    @GetMapping("/getlinkinfo")
    public Result getlinkinfo(@RequestParam String link) {
        File file = fileservice.sharelinkinfo(link);
        return Result.success(file);
    }
}
