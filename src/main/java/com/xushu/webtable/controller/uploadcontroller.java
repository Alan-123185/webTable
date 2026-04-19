package com.xushu.webtable.controller;

import com.xushu.webtable.common.File;
import com.xushu.webtable.common.Result;
import com.xushu.webtable.common.uploadinfo;
import com.xushu.webtable.service.uploadService;
import com.xushu.webtable.utils.CurrentHolder;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;  // 这才是 Spring 的 @Value
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/Mydisk")
public class uploadcontroller {
    @Value("${path.pathhead}")
    private String pathhead;
    @Autowired
    private uploadService uploadservice;
    @PostMapping("/upload")
    public Result upload(@RequestParam MultipartFile file,
                         @RequestParam(value = "parentId",
                                 required = false,
                                 defaultValue = "0") Integer parentId) throws IOException {
        // 1. 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains("\\")) {
            originalFilename = originalFilename.substring(originalFilename.lastIndexOf("\\") + 1);
        }
        
        // 2. 计算文件 MD5（用于秒传判断和作为磁盘文件名）
        String md5 = DigestUtils.md5Hex(file.getInputStream());
        
        // 3. 获取当前用户ID和文件大小
        Integer userId = CurrentHolder.get();
        long fileSize = file.getSize();

        //获取扩展名
        String extName=originalFilename.substring(originalFilename.lastIndexOf(".")+1);
        
        // 4. 构建磁盘存储路径：统一存到 pathhead 目录下，文件名就是 MD5
        String diskPath = pathhead + md5+"."+extName;
        
        // 5. 创建 File 对象（用于数据库记录）
        File f = new File(md5, originalFilename, userId, fileSize, diskPath, LocalDateTime.now(), parentId,0);
        
        // 6. 调用 Service 处理上传逻辑（秒传判断 + 保存）
        uploadservice.upload(f, file.getBytes(), md5, diskPath);
        
        log.info("用户{}上传文件: {}, MD5: {}", userId, originalFilename, md5);
        return Result.success(f);
    }
}
