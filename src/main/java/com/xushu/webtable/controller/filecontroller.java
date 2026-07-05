package com.xushu.webtable.controller;

import com.xushu.webtable.anno.Log;
import com.xushu.webtable.anno.Role;
import com.xushu.webtable.common.*;
import com.xushu.webtable.service.fileService;
import com.xushu.webtable.utils.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import static com.xushu.webtable.common.Result.success;

/**
 *
 * 我发现我目前的系统有一个大问题，
 * 以后可能需要引入一个在线编辑系统
 *
 */
@Slf4j
@RestController
@RequestMapping("/Mydisk")
@Tag(name = "文件管理", description = "文件管理相关接口")
public class filecontroller {
    @Autowired
    private fileService fileservice;
    @GetMapping("/select")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary  = "根据文件名查询文件", description = "返回一个selectFileBean对象")
    public Result select(@Parameter(description = "文件名")@RequestParam(defaultValue = "") String originalFileName,
                         @Parameter(description = "页码")@RequestParam(defaultValue = Const.FILE_QUERY_DEFAULT_PAGE+"") int page,
                         @Parameter(description = "每页数量")@RequestParam(defaultValue = Const.FILE_QUERY_DEFAULT_SIZE+"") int number,
                         @Parameter(description = "父级id")@RequestParam(defaultValue = Const.PARENT_ID_ROOT+"") int parentId){
        selectFileBean files = fileservice.select(originalFileName, CurrentHolder.get(), page, number,parentId);
        log.info("{}查询",originalFileName);
        return success(files);
    }


    @Log(value="删除文件",operationType = Log.OperationType.DELETE)
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "根据文件id批量删除文件", description = "删除文件")
    @DeleteMapping("/delete")
    public Result delete(@Parameter(description = "文件id列表")@RequestBody List<Integer> ids) {
        fileservice.softdelete(ids);
        return Result.success();
    }

    @Log(value="管理员强制删除任意文件",operationType = Log.OperationType.DELETE)
    @Role(value = {Const.ROLE_ADMIN})
    @Operation(summary = "管理员强制删除任意文件", description = "返回一个Integer")
    @PostMapping("/admin/forcedelete")
    public Result forceDelete(@Parameter(description = "文件id列表") @RequestBody List<Integer> ids) {
        fileservice.forceDelete(ids);
        return Result.success();
    }

    @Log(value="分享文件",operationType = Log.OperationType.SHARE)
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "根据文件id分享文件，生成链接", description = "返回一个shareinfo对象")
    @PostMapping("/share")
    public Result share(@Parameter(description = "文件id") @RequestParam Integer fileId){
        shareinfo share = fileservice.share(fileId);
        return Result.success(share);
    }

    @Log(value="根据链存储文件" ,operationType = Log.OperationType.STORE)
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "根据文件链接存储文件", description = "返回一个Integer")
    @GetMapping("/store/{link}")
    public Result store(@Parameter(description="链接名")@PathVariable String link,
                        @Parameter(description = "父级id，如果是0默认存储到根目录下") @RequestParam(defaultValue = "0") Integer parentId,
                        HttpServletResponse response){
       // shareinfo linkinfo = fileservice.findlink(link);
        Integer panlink = fileservice.store(link,parentId);
        if(panlink== Const.STORE_LINK_ERROR)
        {
            log.info("链接错误");
            response.setStatus(Const.HTTP_NOT_FOUND);
            return Result.error("链接错误或者文件已被删除");
        }
        if(panlink== Const.STORE_LINK_EXPIRED)
        {
            log.info("链接失效");
            response.setStatus(Const.HTTP_UNAUTHORIZED);
            return Result.error("链接失效");
        }
        if(panlink== Const.STORE_FILE_EXISTS){
            log.info("文件已存在");
            return Result.error("文件已存在");
        }
        if(panlink== Const.STORE_SPACE_INSUFFICIENT){
            log.info("剩余空间不足");
            return Result.error("剩余空间不足");
        }
        log.info("成功转存");
        return Result.success();
    }
    @Log(value="下载文件",operationType = Log.OperationType.DOWNLOAD)
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "下载文件", description = "根据文件ID下载文件到本地")
    @GetMapping("/download/")
    public Result download(@Parameter(description = "文件ID") @RequestParam Integer fileId,
                         HttpServletResponse response){
            fileservice.download(fileId, response);
            return Result.success();
    }

    @Log(value="批量下载文件",operationType = Log.OperationType.DOWNLOAD)
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @Operation(summary = "批量下载文件", description = "根据文件ID压缩文件并且到本地")
    @GetMapping("/batchdownload/")
    public Result batchdownload(@Parameter(description = "文件ID列表") @RequestParam List<Integer> ids,
                                HttpServletResponse response) {
        fileservice.batchDownload(ids, response);
        return Result.success();
    }

    //一定要用户存储之后才能预览！！
    @Log(value="预览文件",operationType = Log.OperationType.PREVIEW)
    @Operation(summary = "预览文件", description = "根据文件ID在线预览文件内容")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @GetMapping("/preview")
    public void preview(@Parameter(description = "文件ID") @RequestParam(value = "fileId",  defaultValue = "null") Integer fileId,
                        @Parameter(description = "链接") @RequestParam(value = "link",  defaultValue = "null") String link,
                        HttpServletResponse response){
        fileservice.preview(fileId,link, response);
    }

    @Log(value="创建文件夹",operationType = Log.OperationType.OTHER)
    @Operation(summary = "创建文件夹", description = "在指定父目录下创建新文件夹")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @PostMapping("/mkdir")
    public Result mkdir(@Parameter(description = "文件夹名称") @RequestParam String fileName,
                        @Parameter(description = "父级ID，默认为0表示根目录") @RequestParam(defaultValue = "0") Integer parentId){
        fileservice.mkdir(fileName,parentId);
        return Result.success(fileName);
    }

    @Log(value="获取分享链接信息",operationType = Log.OperationType.OTHER)
    @Operation(summary = "获取分享链接信息", description = "根据分享链接获取对应的文件信息")
    @GetMapping("/getlinkinfo")
    public Result getlinkinfo(@Parameter(description = "分享链接标识") @RequestParam String link) {
        File file = fileservice.sharelinkinfo(link);
        return Result.success(file);
    }
    @Log(value="移动文件",operationType = Log.OperationType.OTHER)
    @Operation (summary = "移动文件", description = "移动文件到指定目录")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @PostMapping("/move")
    public Result move(@Parameter(description = "文件ID") @RequestParam Integer fileId,
                       @Parameter(description = "父级目录ID") @RequestParam Integer parentId) {
        fileservice.move(fileId, parentId);
        return Result.success();
    }

    @Log(value="重命名文件",operationType = Log.OperationType.RENAME)
    @Operation(summary = "重命名文件", description = "修改指定文件的名称")
    @Role(value = {Const.ROLE_ADMIN, Const.ROLE_USER})
    @GetMapping("/rename")
    public Result rename(@Parameter(description = "文件ID") @RequestParam Integer fileId,
                         @Parameter(description = "新的文件名称") @RequestParam String fileName) {
        fileservice.rename(fileId, fileName);
        return Result.success(fileName);
    }


}
