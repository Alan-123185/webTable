package com.xushu.webtable.service.maker;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.File;
import com.xushu.webtable.common.User;
import com.xushu.webtable.common.selectFileBean;
import com.xushu.webtable.common.shareinfo;
import com.xushu.webtable.mapper.fileMapper;
import com.xushu.webtable.mapper.recycleBinMapper;
import com.xushu.webtable.mapper.uploadMapper;
import com.xushu.webtable.service.fileService;
import com.xushu.webtable.service.recycleBinService;
import com.xushu.webtable.utils.CurrentHolder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class fileServicemake implements fileService {
//    @Autowired
//    private RedisLockTool redisLockTool;
    @Value("${user.volume}")
    private Long volume;
    @Autowired
    private recycleBinMapper recycleBinMapper;
    @Autowired
    private fileMapper mapper;
    @Autowired
    private uploadMapper uploadmapper;
    @Value("${extName.type}")
    private String extName;
    @Value("${path.pathhead}")
    private String path;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private recycleBinService recycleBinService;

    @Override
    public selectFileBean select(String originalFileName, Integer userId, int page, int number, Integer parentId) {
        PageHelper.startPage(page, number); // 开启分页助手，设置当前页码和每页条数
        List<File> files = mapper.selectFile(originalFileName, userId, parentId);// 执行数据库查询，获取员工列表
        Page<File> p = (Page<File>) files; // 将查询结果强制转换为Page对象，以获取分页信息
        return new selectFileBean(p.getResult(), p.getTotal()); // 返回封装好的分页结果对象，包含数据列表和总记录数
    }

    /**
     * 这个软删除的代码一直没想好！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
     * @param ids 多项删除的文件id集合
     *
     *
     *
     *
     * 经过反复思考，发现softdelete方法里面不需要提供任何删除操作，仅需两个步骤
     *            1.首先，把文件记录添加到回收站
     *            2，然后，更新用户已用空间
     *            3.所有删除操作移动到回收站的彻底删除方法里面
     *
     */
    @Override
    @Transactional
    public void softdelete(List<Integer> ids) {
        // 创建一个集合，用于存储所有需要删除的数据库记录ID（包括文件记录和文件夹记录）
        Set<Integer> id0s = new HashSet<>();
        // 创建一个列表，用于暂存根据传入ID查询到的文件对象
        List<File> files = new ArrayList<>();
        // 创建一个集合，用于存储经过递归处理后，实际需要处理的所有文件对象（展开文件夹后的所有子文件）
        Set<File> onlyid0s = new HashSet<>();
        // 遍历传入的需要删除的文件/文件夹ID列表
        for (Integer id : ids) {
            // 根据ID查询文件信息并添加到列表中
            files.add(mapper.selectFileById(id));
            // 如果当前ID对应的是文件夹（isFolder==1），则将其ID加入待删除集合
            if (mapper.selectFileById(id).getIsFolder() == Const.FILE_TYPE_FOLDER) mapper.deleteSingleFile(id);//是文件夹不管，直接删除
        }
        // 调用递归方法toOnly，将文件夹展开，获取所有需要删除的具体文件对象，并存入onlyid0s集合，这个集合里面不包含文件夹
        onlyid0s = toOnly((ArrayList<File>) files, onlyid0s);
        // 遍历所有需要实际处理（删除或减少引用）的文件对象
        for (File f : onlyid0s) {
            // 获取当前文件的ID
            Integer id = f.getId();
            // 权限校验：如果文件的所有者ID与当前登录用户ID不一致，则抛出403权限错误异常
            if (!f.getUserId().equals(CurrentHolder.get()))
                //权限错误
                throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
            // 获取文件的MD5值（即文件名在底层存储中的标识）
            String md5 = f.getFileName();
            // 根据MD5值在上传记录表中查询对应的原始文件记录ID（id0）
            Integer id0 = uploadmapper.selectByMd5(md5).getId();
            LocalDateTime time = LocalDateTime.now();
            time = time.plusDays(Const.RECYCLE_BIN_EXPIRY_DAYS);
            recycleBinMapper.recyclefile(f, time);
            // 如果当前逻辑文件ID等于原始文件记录ID，说明这是该物理文件的最后一个关联记录或主记录，清理所有者信息
            if (id.equals(id0)) {
                mapper.cleanower(id0);
            }
            // 再次根据MD5查询原始文件记录，获取当前的引用计数（refCount）
            Integer refcount = uploadmapper.selectByMd5(md5).getRefCount();
            //只能查找>0的！不合理！
            // 如果引用计数为1，说明这是最后一个指向该物理文件的逻辑记录，需要彻底删除物理文件和数据库记录
            if (refcount == 1) {
                //是最后一个文件
                /**
                 * 这个地方为了适应回收站机制和逻辑删除，不能彻底删除文件，只能将文件从file中删除并且移入recycle_bin里面，并减少引用计数
                 */

                //完全没有文件,删磁盘文件，最后一个文件
                // 获取该文件在磁盘上的存储路径
//                String path=mapper.selectFileById(id0).getPath();
//                try{
//                    // 尝试删除磁盘上的物理文件
//                    Files.delete(Path.of(path));
//                }catch (IOException e) {
//                    // 如果文件不存在或IO异常，抛出404异常
//                    throw new ManageException(404,"文件不存在");
//                }
                //将原始文件记录ID加入待删除集合
                id0s.add(id0);
                // 将当前逻辑文件ID加入待删除集合
                id0s.add(id);    ////这里防止删掉原始文件，当最后一个删除者是文件初始作者的时候
                // 删除分享链接等关联数据
                mapper.deletelink(id0);
            }
            // 如果引用计数大于1，说明还有其他用户或记录引用该物理文件，只需删除当前逻辑记录
            else if (refcount > 1) {
                // 如果当前ID不是原始文件记录ID（即只是普通的一个副本引用），则将其加入待删除集合
                if (!id.equals(id0))
                    id0s.add(id);
            }
            // 无论哪种情况，都将原始文件记录的引用计数减1
            //存入cycle bin
            uploadmapper.updateRefCount2(id0);
            /**
             * 这个时候，被移动到回收站的那个文件实际ref-count为0，
             * 但是仍然存在在file表里面，
             * 回收站操作:
             * 1。清空，同时需要删除file表里面的数据
             * 2。回复,恢复文件，同时需要将file表里面的ref-count=1
             *
             */
        }
        // 初始化总文件大小变量，用于计算释放的空间
        Long sum = 0L;
        // 遍历所有需要删除的记录ID
        for (Integer id : id0s) {
            // 查询对应的文件对象
            File f = mapper.selectFileById(id);
            // 累加文件大小
            if(f.getIsFolder() == Const.FILE_TYPE_FILE)
            sum += f.getFileSize();

            mapper.deletelink(id);
        }
        // 如果有需要删除的记录
        if (!id0s.isEmpty()) {
            // 查询当前用户的信息
            User u = uploadmapper.selectUserById(CurrentHolder.get());
            // 更新用户的已用空间：原空间减去删除文件的总大小
            u.setVolume(u.getVolume() - sum);
//            // 将更新后的用户空间信息保存回数据库
            uploadmapper.updateVolume(u);
//            // 批量删除数据库中的文件记录
            mapper.deleteFile(new ArrayList<>(id0s));//删除记录
        }
    }

    @Override
    public shareinfo share(Integer fileId) {
        if (!mapper.selectFileById(fileId).getUserId().equals(CurrentHolder.get())) {
            throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
        }
        if (mapper.selectFileById(fileId).getIsFolder().equals(Const.FILE_TYPE_FOLDER)) {
            throw new ManageException(Const.HTTP_BAD_REQUEST, "文件夹不能分享");
        }
        String link = UUID.randomUUID().toString().substring(0, Const.SHARE_LINK_LENGTH);
        LocalDateTime time = LocalDateTime.now();
        LocalDateTime time1 = time.plusDays(Const.SHARE_EXPIRY_DAYS);
        //查找md5
        String md5 = mapper.selectFileById(fileId).getFileName();
        //根据MD5确定原始文件id
        Integer fileId0 = uploadmapper.selectByMd5(md5).getId();
        mapper.share(fileId0, link, time1);
        return new shareinfo(link, fileId0, time1);
    }

    @Override
    public File sharelinkinfo(String link) {
        shareinfo linkinfo = mapper.findlink(link);
        if (linkinfo != null && linkinfo.getLoseTime().isAfter(LocalDateTime.now())) {
            return mapper.selectFileById(linkinfo.getFileId());
        }
        return null;
    }

    @Override
    public Integer rename(Integer fileId, String newName) {
        File f=mapper.selectFileById(fileId);
        String extname=getExtName(f.getOriginalFileName());
        newName+=extname;
        return mapper.rename(fileId, newName);
    }

    @Override
    public void move(Integer fileId, Integer parentId) {
        File file = mapper.selectFileById(fileId);
        File folder = mapper.selectFileById(parentId);
        if (folder == null) {
            throw new ManageException(Const.HTTP_NOT_FOUND, "文件夹不存在");
        }
        if (file == null) {
            throw new ManageException(Const.HTTP_NOT_FOUND, "文件不存在");
        }
        if (!file.getUserId().equals(CurrentHolder.get())||!folder.getUserId().equals(CurrentHolder.get())){
            throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
        }
        mapper.movedir(fileId, parentId);
    }

    @Override
    public Integer store(String link, Integer parentId) {
        if (!parentId.equals(Const.PARENT_ID_ROOT)&& !mapper.selectFileById(parentId).getUserId().equals(CurrentHolder.get())) {
            throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
        }
        shareinfo linkinfo = mapper.findlink(link);
        if (linkinfo == null) return Const.STORE_LINK_ERROR;//链接错误或者文件已经被删除
        if (linkinfo.getLoseTime().isBefore(LocalDateTime.now())) return Const.STORE_LINK_EXPIRED;
        //连接超时
        Integer fileId = linkinfo.getFileId();
        File existFile = mapper.selectFileById(fileId);
        Integer userId = CurrentHolder.get();
        User user = uploadmapper.selectUserById(userId);
        if (user.getVolume() + existFile.getFileSize() > volume) {
            return Const.STORE_SPACE_INSUFFICIENT;//剩余空间不足
        }
        if (existFile == null) return Const.STORE_LINK_ERROR;
        String md5 = existFile.getFileName();
        File fnow = mapper.selectFileById(fileId);
        if (fnow.getUserId().equals(CurrentHolder.get()) && fnow.getFileName().equals(md5)) {
            return Const.STORE_FILE_EXISTS;//文件已存在
        }
        //File  = uploadmapper.selectByMd5(md5);
        uploadmapper.updateRefCount(existFile.getId());
        uploadmapper.insertRecord(new File(md5, existFile.getOriginalFileName(), userId, existFile.getFileSize(), existFile.getPath(), LocalDateTime.now(), Const.REFCOUNT_SECOND_UPLOAD, parentId, Const.FILE_TYPE_FILE));
        uploadmapper.updateVolume(user);//更新用户已用空间
        return Const.STORE_SUCCESS;
    }

    /**
     * 下载文件
     * <p>
     * @param fileId
     * @param response
     */
    @Override
    public void download(Integer fileId, HttpServletResponse response) {
        //有权限
        File file = mapper.selectFileById(fileId);
        if (file == null) throw new ManageException(Const.HTTP_NOT_FOUND, "文件不存在");
        if (!file.getUserId().equals(CurrentHolder.get())) throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
        //先给文件引用数加1，防止被删除
        //   uploadmapper.updateRefCount(fileId);
//        //下载逻辑
//        String value = redisLockTool.tryLock(Const.REDIS_DOWNLOAD_LOCK_KEY + fileId,
//                Const.LOCK_LEASE_TIME_SECONDS,
//                Const.LOCK_RETRY_COUNT,
//                Const.LOCK_RETRY_INTERVAL_MS);
        redisTemplate.opsForValue().increment(Const.REDIS_DOWNLOAD_LOCK_KEY + fileId);//如果在下载的过程中被物理删除，这个代表的意义是一个文件正在被多少人下载
        redisTemplate.expire(Const.REDIS_DOWNLOAD_LOCK_KEY + fileId, 1, TimeUnit.HOURS); // 兜底过期，防止死key
        // 4. 再次检查文件状态（防止在获取锁之前被删除）
        file = mapper.selectFileById(fileId);
        if (file == null) {
            throw new ManageException(Const.HTTP_NOT_FOUND, "文件不存在");
        }
        Path path = Path.of(file.getPath());//获取文件路径对象
        if (!Files.exists(path)) {
            throw new ManageException(Const.HTTP_NOT_FOUND, "文件不存在");
        }
        //设置包装信息，告诉浏览器下载文件
        try {
            response.setContentType(Const.CONTENT_TYPE_OCTET_STREAM);//设置响应头，告诉浏览器下载文件
            String fileName = URLEncoder.encode(file.getOriginalFileName(),
                    Const.CHARSET_UTF8).replaceAll("\\+", "%20");//解决中文乱码
            response.setHeader(Const.HEADER_CONTENT_DISPOSITION, Const.DISPOSITION_ATTACHMENT + fileName);//强制浏览器以附件形式下载，并且指定文件名
            response.setContentLength(file.getFileSize().intValue());//提前告诉浏览器文件大小，使浏览器出现下载进度条
            //板运数据
            Files.copy(path, response.getOutputStream());
        } catch (IOException e) {
            throw new ManageException(Const.HTTP_INTERNAL_ERROR, "下载失败");
        } finally {
            //  uploadmapper.updateRefCount2(fileId);//无论是否成功，将文件引用数减1
            //  redisLockTool.unlock(Const.REDIS_DOWNLOAD_LOCK_KEY + fileId, value);
            redisTemplate.opsForValue().decrement(Const.REDIS_DOWNLOAD_LOCK_KEY + fileId);
        }
    }

    @Override
    public void batchDownload(List<Integer> ids, HttpServletResponse response) {
        // 创建一个临时文件，用于存储下载的文件
        response.setContentType(Const.CONTENT_TYPE_BATCH_DOWNLOAD);
        String fileName="download.zip";
        response.setHeader(Const.HEADER_CONTENT_DISPOSITION, Const.DISPOSITION_ATTACHMENT + fileName);
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Integer fileId : ids) {
                File fileInfo = mapper.selectFileById(fileId); // 查数据库得到物理路径
                if (fileInfo == null) {
                    throw new ManageException(Const.HTTP_NOT_FOUND, "文件不存在");
                }
                if (!fileInfo.getUserId().equals(CurrentHolder.get())) {
                    throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
                }
                redisTemplate.opsForValue().increment(Const.REDIS_DOWNLOAD_LOCK_KEY + fileId);
                redisTemplate.expire(Const.REDIS_DOWNLOAD_LOCK_KEY+fileId, 1, TimeUnit.HOURS); // 兜底过期
                Path filePath = Paths.get(fileInfo.getPath());
                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    // 添加 ZIP 条目，条目名可以用原始文件名（避免重名，可加前缀）
                    zos.putNextEntry(new ZipEntry(fileInfo.getOriginalFileName())); // 添加 ZIP 条目，条目名可以用原始文件名（避免重名，可加前缀）
                    Files.copy(filePath, zos); // 直接将文件内容拷贝到 ZIP 输出流
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new ManageException(Const.HTTP_INTERNAL_ERROR, "下载失败");
            // 因响应已提交，无法再返回错误页面，需记录日志并可能抛出运行时异常
        }finally{
            for (Integer id : ids) {
                redisTemplate.opsForValue().decrement(Const.REDIS_DOWNLOAD_LOCK_KEY + id);
            }
        }
    }

    /**
     *
     *
     * 这里强力删除的代码还没写！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
     *
     *
     *
     * @param fileIds
     */
    @Override
    public void forceDelete(List<Integer> fileIds) {

    }

    /**
     * 这个方法用于根据文件ID和响应对象，实现文件预览功能，仅限于文件所有者预览
     * @param fileId
     * @param response
     */
    @Override
    public void preview(Integer fileId, String link,HttpServletResponse response) {
        // 将配置文件中允许预览的扩展名字符串转换为列表
        List<String> list = Arrays.asList(extName);
        // 根据文件ID查询文件信息
        File file = mapper.selectFileById(fileId);
        // 获取文件的原始名称
        String originalFileName = file.getOriginalFileName();
        // 检查文件名中是否包含扩展名分隔符，若不包含则抛出异常
        if (!originalFileName.contains(".")) {
            throw new ManageException(Const.HTTP_BAD_REQUEST, "无法识别文件格式，请下载后查看");
        }
        // 根据文件的MD5值查询原始文件记录（用于获取真实存储路径或验证）
        File originalFile = uploadmapper.selectByMd5(file.getFileName());
        // 提取文件扩展名
        String extname = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        // 权限校验：判断当前用户是否为文件所有者，或者该文件是否存在有效的分享链接
        if (file!=null&&file.getUserId().equals(CurrentHolder.get())||link!=null&&sharelinkinfo(link)!=null) {
            // 有权限访问
            // 增加文件引用计数，防止在预览过程中文件被其他线程删除
            uploadmapper.updateRefCount(fileId);
            // 构建文件路径对象
            try {
                Path path = Path.of(file.getPath());
                // 检查物理文件是否存在，若不存在则抛出404异常
                if (!Files.exists(path)) throw new ManageException(Const.HTTP_NOT_FOUND, "文件丢失");
                // 定义内容类型变量
                String contentType;
                // 根据文件扩展名确定响应的Content-Type
                switch (extname.toLowerCase()) {
                    case "jpg":
                    case "jpeg":
                        contentType = Const.CONTENT_TYPE_JPEG;
                        break;
                    case "png":
                        contentType = Const.CONTENT_TYPE_PNG;
                        break;
                    case "gif":
                        contentType = Const.CONTENT_TYPE_GIF;
                        break;
                    case "bmp":
                        contentType = Const.CONTENT_TYPE_BMP;
                        break;
                    case "webp":
                        contentType = Const.CONTENT_TYPE_WEBP;
                        break;
                    case "pdf":
                        contentType = Const.CONTENT_TYPE_PDF;
                        break;
                    case "txt":
                        contentType = Const.CONTENT_TYPE_TXT;
                        break;
                    case "md":
                        contentType = Const.CONTENT_TYPE_MD;
                        break;
                    default:
                        // 如果扩展名在配置的允许列表中，则作为二进制流处理，否则抛出不支持预览异常
                        if (list.contains(extname)) {
                            contentType = Const.CONTENT_TYPE_OCTET_STREAM;
                        } else {
                            throw new ManageException(Const.HTTP_INTERNAL_ERROR, "该格式暂不支持在线预览");
                        }
                }
                // 设置响应内容类型
                response.setContentType(contentType);
                // 对文件名进行URL编码，处理中文乱码问题，并将空格替换为%20
                String fileName = URLEncoder.encode(file.getOriginalFileName(), Const.CHARSET_UTF8).replaceAll("\\+", "%20");
                // 设置响应头，Content-Disposition为inline表示浏览器尝试直接预览而非下载
                response.setHeader(Const.HEADER_CONTENT_DISPOSITION, Const.DISPOSITION_INLINE + fileName);
                // 设置响应内容长度，以便浏览器显示下载/加载进度条
                response.setContentLength(file.getFileSize().intValue());
                // 将文件数据复制到响应输出流中
                Files.copy(path, response.getOutputStream());
            } catch (IOException e) {
                // 捕获IO异常并抛出自定义异常
                throw new ManageException(Const.HTTP_INTERNAL_ERROR, "下载失败");
            } finally {
                // 无论操作成功与否，都在finally块中将文件引用计数减1，恢复状态
                uploadmapper.updateRefCount2(fileId);
            }
        } else {
            // 若无权限，抛出403禁止访问异常
            throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
        }
    }

    @Override
    public void mkdir(String originalFileName,Integer parentId) {
        //这里改了ref_count，因为我在delete那里写死了，所以那里要改
        File f=new File("",originalFileName,CurrentHolder.get(),null,path+originalFileName,LocalDateTime.now(),Const.REFCOUNT_SECOND_UPLOAD,parentId,Const.FILE_TYPE_FOLDER);
        uploadmapper.insertRecord(f);
    }


//写一个递归函数，将所有子文件夹的id0都加入set中

    public  Set<File> toOnly(ArrayList<File> list,Set<File> onlyid0s){
        for (File file : list) {
            if(file.getIsFolder()==Const.FILE_TYPE_FILE){
                onlyid0s.add(file);
            }
            else {
                List<File> files =mapper.selectByParentId(file.getId());
                toOnly((ArrayList<File>) files, onlyid0s);
            }
        }
        return onlyid0s;
    }

    public String getExtName(String originalFilename){
        if(originalFilename==null)return "";
        String extName = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx >= 0) extName = "." + originalFilename.substring(dotIdx + 1);
        return extName;
    }
}
