package com.xushu.webtable.controller;

import com.xushu.webtable.ManageException;
import com.xushu.webtable.anno.Log;
import com.xushu.webtable.anno.Role;
import com.xushu.webtable.common.*;
import com.xushu.webtable.service.uploadService;
import com.xushu.webtable.utils.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;  // 这才是 Spring 的 @Value
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/Mydisk/upload")
@Tag(name = "专门用来管理文件上传")
public class uploadcontroller {
    @Value("${path.pathhead}")
    private String pathhead;
    @Autowired
    private uploadService uploadservice;

    @Log(value="检查文件是否已经存在" ,operationType=Log.OperationType.UPLOAD)
    @PostMapping("/check")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "检查文件是否已存在", description = "如果存在直接秒传")
    public Result check(
            @Parameter(description = "自定义检查青丘对象") @RequestBody CheckRequest checkRequest) {
        CheckResult checkresult = uploadservice.check(checkRequest);
        return Result.success(checkresult);
    }


    @Log(value="对单个片段文件上传" ,operationType=Log.OperationType.UPLOAD)
    @PostMapping("/chunk")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "上传文件到指定文件夹", description = "上传文件到指定文件夹,默认是根目录下")
    public Result chunk(@Parameter(description = "上传的文件分片") @RequestParam MultipartFile file,
                        @Parameter(description = "上传任务ID") @RequestParam String taskId,
                        @Parameter(description = "上传的文件的索引") @RequestParam Integer index) {
        uploadservice.chunk(file, taskId, index);
        return Result.success();
    }

    @Log(value="合并文件" ,operationType=Log.OperationType.UPLOAD)
    @PostMapping("/merger")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "合并文件", description = "合并文件或者实现秒传逻辑")
    public Result merger(
            @Parameter(description = "检查结果对象") @RequestBody CheckResult checkResult,
            @Parameter(description = "上传文件的临时存储路径") @RequestParam Integer parentId) {
        uploadservice.merger(checkResult, parentId);
        return Result.success();
    }

}
        // 1. 获取原始文件名
//        String originalFilename = file.getOriginalFilename();
//        if (originalFilename != null && originalFilename.contains("\\")) {
//            originalFilename = originalFilename.substring(originalFilename.lastIndexOf("\\") + 1);
//        }
//
//        // 2. 计算文件 MD5（用于秒传判断和作为磁盘文件名）
//        String md5 = DigestUtils.md5Hex(file.getInputStream());
//
//        // 3. 获取当前用户ID和文件大小
//        Integer userId = CurrentHolder.get();
//        long fileSize = file.getSize();
//
//        //获取扩展名
//        String extName=originalFilename.substring(originalFilename.lastIndexOf(".")+1);
//
//        // 4. 构建磁盘存储路径：统一存到 pathhead 目录下，文件名就是 MD5
//        String diskPath = pathhead + md5+"."+extName;
//
//        log.info("上传配置 - pathhead: {}", pathhead);
//        log.info("上传配置 - diskPath: {}", diskPath);
//        log.info("上传配置 - 文件是否存在: {}", new java.io.File(pathhead).exists());
//
//        // 5. 创建 File 对象（用于数据库记录）
//        File f = new File(md5, originalFilename, userId, fileSize, diskPath, LocalDateTime.now(), parentId,0);
//
//        // 6. 调用 Service 处理上传逻辑（秒传判断 + 保存）
//        try {
//            uploadservice.upload(f, file.getBytes(), md5, diskPath);
//        } catch (IOException e) {
//            throw new ManageException(500,"上传失败，请稍后再试");
//        }
//        log.info("用户{}上传文件: {}, MD5: {}", userId, originalFilename, md5);
//        return Result.success(f);
//    }
//}
