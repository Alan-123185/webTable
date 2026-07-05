package com.xushu.webtable.service.maker;

import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.*;
import com.xushu.webtable.mapper.uploadMapper;
import com.xushu.webtable.service.uploadService;
import com.xushu.webtable.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 传输：
 * 1.首先检测文件是否已存在，能不能秒传，如果不能妙传，先创建该文件的temp 目录下
 * 2.用upload方法实现上传逻辑，首先全部上传到temp目录下，然后整合到正式目录下
 *
 */
@Slf4j
@Service
public class uploadServicemake implements uploadService {

    private static final String FRONT_KEY = Const.REDIS_UPLOAD_KEY_PREFIX;

    @Autowired
    StringRedisTemplate stringredisTemplate;

    @Autowired
    private uploadMapper mapper;
    @Value("${user.volume}")
    private Long volume;
    @Value("${path.pathhead}")
    private String pathhead;

    /**
     * 单个文件上传
     *
     * @param file     分片文件对象
     * @param taskId   任务ID（由check方法返回）
     * @param index    分片文件的索引（从0开始）
     */
    @Override
    public void chunk(MultipartFile file, String taskId, Integer index) {
        // 当check方法返回false的时候需要手动上传
        // 构建完整临时路径：pathhead + temp + upload + taskId
        Path path = Paths.get(pathhead, Const.PATH_TEMP, Const.PATH_UPLOAD, taskId);
        Path path0 = path.resolve(index + ".tmp");//临时文件路径
        Path path1 = path.resolve(String.valueOf(index));//目标文件路径
        try {
            //尝试保存文件
            Files.createDirectories(path);
            file.transferTo(path0.toFile());//写入磁盘
            Files.move(path0, path1, StandardCopyOption.ATOMIC_MOVE);
            log.info("分片上传成功: taskId={}, index={}, path={}", taskId, index, path1);
        } catch (IOException e) {
            //单个分片保存失败，先删干净
            try {
                Files.deleteIfExists(path0);
            } catch (IOException e1) {
                //删除失败
                log.warn("临时分片文件删除失败，请重试");
            }
            log.error("分片上传失败: taskId={}, index={}, error={}", taskId, index, e.getMessage());
            throw new ManageException("分片上传失败，请重试");
        }
        ////将进度记录到缓存里面
        String key = FRONT_KEY + taskId + ":";
        stringredisTemplate.opsForSet().add(key, String.valueOf(index));
        stringredisTemplate.expire(key, Const.REDIS_UPLOAD_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 文件上传前检查（秒传检测、断点续传支持）
     * <p>
     * 逻辑流程：
     * 1. 校验用户剩余存储空间是否充足。
     * 2. 根据文件 MD5 查询数据库，若存在则返回秒传结果（exist=true）。
     * 这里我决定采用redis+lua脚本解决同时上传同一个文件的并发问题
     * 3. 若不存在，检查请求中是否携带 taskId：
     * - 若携带 taskId，从 Redis 获取已上传的分片索引列表，返回断点续传结果。
     * - 若未携带 taskId，生成新的 taskId，创建临时存储目录，返回全新上传结果。
     * </p>
     *
     * @param checkRequest 包含文件 MD5、文件名、总分片数、文件大小及可选 taskId 的检查请求对象
     * @return CheckResult 检查结果，包含是否秒传、taskId、已上传分片列表等信息
     * @throws ManageException 当剩余空间不足或临时目录创建失败时抛出
     */
    @Override
    public CheckResult check(CheckRequest checkRequest) {
        // 获取当前用户ID并查询用户信息
        Integer userId = CurrentHolder.get();
        User user = mapper.selectUserById(userId);
        
        // 检查用户剩余空间是否足够
        if (user.getVolume() + checkRequest.getFileSize() > volume) {
            throw new ManageException(Const.HTTP_BAD_REQUEST, "剩余空间不足");
        }
        
        // 获取请求参数
        String md5 = checkRequest.getMd5(); // 文件MD5值
        String taskid = checkRequest.getTaskId(); // 任务ID(断点续传时不为空)
        String newtaskid = UUID.randomUUID().toString().replace("-", ""); // 生成新任务ID
        Integer totalChunks = checkRequest.getTotalChunks(); // 总分片数
        int ttl = Const.REDIS_UPLOAD_TTL_TIME; // Redis缓存时间
        
        // 准备Redis键
        String fileKey = Const.REDIS_FILE_MD5_KEY + md5; // 文件MD5对应的键
        String taskInfoKey = Const.REDIS_FILE_TASK_INFO_KEY + (taskid != null ? taskid : newtaskid); // 任务信息键
        
        // 准备Lua脚本参数
        List<String> keys = Arrays.asList(fileKey, taskInfoKey); // Redis键列表
        Object[] args = {
                md5,                              // ARGV[1]: 文件MD5
                taskid != null ? taskid : "",     // ARGV[2]: 原任务ID(如果有)
                newtaskid,                        // ARGV[3]: 新任务ID
                String.valueOf(userId),           // ARGV[4]: 用户ID
                String.valueOf(totalChunks),      // ARGV[5]: 总分片数
                String.valueOf(ttl)               // ARGV[6]: 缓存时间
        };
        
        // 设置Lua脚本
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("check.lua"))); // 加载Lua脚本
        script.setResultType(List.class); // 设置返回类型
        
        // 执行Lua脚本并获取结果
        List<String> result = stringredisTemplate.execute(script, keys, args);
        
        // 根据Lua脚本返回状态处理不同情况
        switch (result.get(0)) {
            case Const.LUA_CHECK_FAST: // 秒传情况
                File existFile1 = mapper.selectByMd5(md5); // 查询已存在文件
                CheckResult cr = new CheckResult(checkRequest.getMd5(),
                        checkRequest.getFileName(),
                        checkRequest.getTotalChunks(),
                        true, // 标记为秒传
                        existFile1.getFileSize(),
                        null,
                        null);
                return cr;
                
            case Const.LUA_CHECK_NEW: // 新文件上传
                File existFile2 = mapper.selectByMd5(md5);
                if(existFile2!=null) { // 二次检查文件是否存在(防止并发问题)
                    CheckResult cr1 = new CheckResult(checkRequest.getMd5(),
                            checkRequest.getFileName(),
                            checkRequest.getTotalChunks(),
                            true, // 标记为秒传
                            existFile2.getFileSize(),
                            null,
                            null);
                    stringredisTemplate.opsForValue().set(Const.REDIS_FILE_MD5_KEY+md5, Const.LUA_CHECK_DONE,30,TimeUnit.DAYS); // 更新Redis状态
                    return cr1;
                }
                // 创建临时目录用于分片上传
                Path path = Paths.get(pathhead, Const.PATH_TEMP, Const.PATH_UPLOAD, result.get(1));
                try {
                    Files.createDirectories(path); // 创建临时目录
                } catch (IOException e) {
                    throw new ManageException("临时目录创建失败");
                }
                return new CheckResult(checkRequest.getMd5(),
                        checkRequest.getFileName(),
                        checkRequest.getTotalChunks(),
                        false, // 非秒传
                        checkRequest.getFileSize(),
                        result.get(1), // 返回任务ID
                        null);
                        
            case Const.LUA_CHECK_WAIT: // 文件正在上传中
                throw new ManageException(Const.HTTP_ACCEPTED, "文件正在上传中");
                
            case Const.LUA_CHECK_RESUME: // 断点续传
                // 获取已上传的分片列表
                List<Integer> uploadedChunks = stringredisTemplate
                        .opsForSet()
                        .members(FRONT_KEY + result.get(1)+ ":")
                        .stream()
                        .map(Integer::parseInt)
                        .toList();
                return new CheckResult(
                        checkRequest.getMd5(),
                        checkRequest.getFileName(),
                        checkRequest.getTotalChunks(),
                        false, // 非秒传
                        checkRequest.getFileSize(),
                        taskid, // 原任务ID
                        uploadedChunks); // 已上传分片列表
                        
            default: // 未知状态
                throw new ManageException("秒传检查异常，未知状态: " + result.get(0));
        }
        // Lua脚本保证了检查过程的原子性
    }

    /**
     * 处理一些文件上传的合并问题！传入的是check方法得到的返回结果
     *
     */
    @Override
    public void merger(CheckResult checkResult, Integer parentId) {
        String md5 = checkResult.getMd5();
        String filename = checkResult.getOrigfileName();
        /**
         * 如果可以秒传
         * 说明数据库里面存在原文件
         */
        User user = mapper.selectUserById(CurrentHolder.get());
        if (checkResult.isExist()) {
            File origfile = mapper.selectByMd5(md5);
            File newfile = new File(md5,
                    filename,
                    CurrentHolder.get(),
                    origfile.getFileSize(),
                    origfile.getPath(),
                    LocalDateTime.now(),
                    0,
                    parentId,
                    0
            );
            user.setVolume(user.getVolume()-origfile.getFileSize());
            mapper.insertRecord(newfile);
            mapper.updateRefCount(origfile.getId());
        }
        /**
         * 如果不能秒传
         * 说明数据库里面不存在该文件
         * 分片文件合并
         *
         */
        else {
            // 非秒传场景下，taskId 绝对不能为 null
            if (!checkResult.isExist() && (checkResult.getTaskId() == null || checkResult.getTaskId().isEmpty())) {
                throw new ManageException("非秒传场景 taskId 不能为空，请检查前端传参");
            }
            String diskPath = pathhead + md5;
            //合并逻辑
            /// /先校验文件是否完整
            List<Integer> uploadedChunks = stringredisTemplate
                    .opsForSet()
                    .members(FRONT_KEY + checkResult.getTaskId() + ":")
                    .stream()
                    .map(Integer::parseInt)
                    .toList();
            if (uploadedChunks.size() != checkResult.getTotalChunks()) {
                throw new ManageException("文件内容残缺");
            }
            Path path = Paths.get(diskPath);
            String extName=getExtName(checkResult.getOrigfileName());
            log.info("开始合并分片: taskId={}, fileName={}, totalChunks={}",
                    checkResult.getTaskId(), checkResult.getOrigfileName(), checkResult.getTotalChunks());
            try {
                md5=mergeChunks(Paths.get(pathhead, Const.PATH_TEMP, Const.PATH_UPLOAD, checkResult.getTaskId()),
                        checkResult.getTotalChunks(),
                        path,
                        extName);
                log.info("分片合并成功: taskId={}, md5={}", checkResult.getTaskId(), md5);
                // 合并后文件在 pathhead/{md5}（无扩展名），重命名为 pathhead/{md5}.{ext}
//                Path finalFileNoExt = Paths.get(pathhead, md5);
                Path finalFileWithExt = Paths.get(pathhead, md5 + "." + extName);
//                if (Files.exists(finalFileNoExt) && !Files.exists(finalFileWithExt)) {
//                    Files.move(finalFileNoExt, finalFileWithExt, StandardCopyOption.ATOMIC_MOVE);
//                } else if (Files.exists(finalFileNoExt) && Files.exists(finalFileWithExt)) {
//                    Files.delete(finalFileNoExt);
//                }diskPath = finalFileWithExt.toString();
                //重新计算一次md5和文件大小
                File newfile = new File(md5,
                        filename,
                        CurrentHolder.get(),
                        Files.size(finalFileWithExt),
                        finalFileWithExt.toString(),
                        LocalDateTime.now(),
                        Const.REFCOUNT_FIRST_UPLOAD,
                        parentId,
                        Const.FILE_TYPE_FILE
                );
                newfile.setCreateTime(LocalDateTime.now());
                mapper.insertRecord(newfile);
                //更新用户存储空间
                user.setVolume(user.getVolume() + newfile.getFileSize());
                //删除临时文件
                //递归删除这个文件夹
                boolean p=deleteDir(Paths.get(pathhead,
                        Const.PATH_TEMP, Const.PATH_UPLOAD, checkResult.getTaskId()));
                if(!p)
                   log.warn("删除临时文件{}失败",Paths.get(pathhead,
                           Const.PATH_TEMP, Const.PATH_UPLOAD, checkResult.getTaskId()));
            } catch (IOException e) {
                log.error("文件合并失败: taskId={}, fileName={}, error={}",
                        checkResult.getTaskId(), checkResult.getOrigfileName(), e.getMessage(), e);
                // 清理临时分片目录
                deleteDir(Paths.get(pathhead, Const.PATH_TEMP, Const.PATH_UPLOAD, checkResult.getTaskId()));
                // 清理合并残留文件
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    log.warn("清理合并残留文件失败: {}", path);
                }
                throw new ManageException("文件合并失败");
            }
            String fileKey =Const.REDIS_FILE_MD5_KEY + md5;
            String taskInfoKey =Const.REDIS_FILE_TASK_INFO_KEY + checkResult.getTaskId();
            List<String> keys = Arrays.asList(fileKey, taskInfoKey);
            DefaultRedisScript<List> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("unlockAfterCheck.lua")));
            stringredisTemplate.execute(script,keys);
            //删除任务信息
            stringredisTemplate.delete(FRONT_KEY + checkResult.getTaskId() + ":");
            stringredisTemplate.delete(Const.REDIS_FILE_TASK_INFO_KEY + checkResult.getTaskId());
        }
        mapper.updateVolume(user);
    }

    /**
     * 合并分片，边写边算 MD5，最终文件以 MD5 命名。
     *
     * @param tempDir     分片临时目录
     * @param totalChunks 分片总数
     * @param destPath    合并时的临时写入路径（之后会被重命名为 MD5）
     * @return 文件的 MD5 值
     * @throws IOException
     */
    private String mergeChunks(Path tempDir, int totalChunks, Path destPath,String extName) throws IOException {
        // 确保目标父目录存在
        Files.createDirectories(destPath.getParent());
        log.info("合并分片开始: tempDir={}, totalChunks={}, destPath={}", tempDir, totalChunks, destPath);

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // MD5 算法必定存在
        }

        // 边写入 destPath，边计算 MD5
        try (OutputStream os = Files.newOutputStream(destPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
             DigestOutputStream dos = new DigestOutputStream(os, md)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = tempDir.resolve(String.valueOf(i));
                if (!Files.exists(chunkFile)) {
                    throw new IOException("分片缺失: chunk " + i);
                }
                // 直接拷贝，数据同时流入 dos 和 md 摘要
                Files.copy(chunkFile, dos);
            }
        } // dos/os 关闭后，md 中保存了完整文件的摘要
        // 获取 MD5 字符串
        String md5 = Hex.encodeHexString(md.digest());
        String finalFileName = extName.isEmpty() ? md5 : md5 + "." + extName;
        Path finalFile = destPath.getParent().resolve(finalFileName);
        // 文件去重：相同内容已经存在，删除临时文件；否则重命名
        /**
         * 这里已经重命名了
         */
        if (Files.exists(finalFile)) {
            Files.delete(destPath);
        } else {
            Files.move(destPath, finalFile);
        }
        log.info("合并分片完成: md5={}, finalFile={}", md5, finalFile);
        return md5;
    }
    //递归删除文件夹的方法
    public boolean deleteDir(Path path) {
        if (!Files.exists(path)) {
            return true;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())  // 先叶子后根
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            return true;
        } catch (IOException e) {
            log.error("递归删除失败: {}", path, e);
            return false;
        }
    }
    public String getExtName(String originalFilename){
        if(originalFilename==null)return "";
        String extName = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx >= 0) extName = originalFilename.substring(dotIdx + 1);
        return extName;
    }
}