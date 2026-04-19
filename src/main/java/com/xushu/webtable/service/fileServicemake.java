package com.xushu.webtable.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xushu.webtable.common.File;
import com.xushu.webtable.common.User;
import com.xushu.webtable.common.selectFileBean;
import com.xushu.webtable.common.shareinfo;
import com.xushu.webtable.mapper.fileMapper;
import com.xushu.webtable.mapper.uploadMapper;
import com.xushu.webtable.utils.CurrentHolder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class fileServicemake implements fileService {
    @Value("${user.volume}")
    private Long volume;
    @Autowired
    private fileMapper mapper;
    @Autowired
    private uploadMapper uploadmapper;
    @Value("${extName.type}")
    private String  extName;
    @Value("${path.pathhead}")
    private String path;

    @Override
    public selectFileBean select(String originalFileName,Integer userId,int page,int number,Integer parentId) {
        PageHelper.startPage(page,number); // 开启分页助手，设置当前页码和每页条数
        List<File> files = mapper.selectFile(originalFileName, userId,parentId);// 执行数据库查询，获取员工列表
        Page<File> p=(Page<File>)files; // 将查询结果强制转换为Page对象，以获取分页信息
        return new selectFileBean(p.getResult(),p.getTotal()); // 返回封装好的分页结果对象，包含数据列表和总记录数
    }
    @Override
    public void delete(List<Integer> ids) throws IOException {
        Set<Integer> id0s=new HashSet<>();
        List<File> files=new ArrayList<>();
        Set<File> onlyid0s=new HashSet<>();
        for (Integer id : ids) {
            files.add(mapper.selectFileById(id));
            if(mapper.selectFileById(id).getIsFolder()==1)id0s.add(id);
        }
        onlyid0s=toOnly((ArrayList<File>) files,onlyid0s);
        for (File f: onlyid0s) {
            Integer id=f.getId();
            if(f.getUserId()!=CurrentHolder.get())
               //权限错误
                throw new RuntimeException("权限错误");
            //获取路径
            String md5=f.getFileName();
            Integer id0=uploadmapper.selectByMd5(md5).getId();
            if(f.getId()==id0){
                mapper.cleanower(id0);
            }
            Integer refcount=uploadmapper.selectByMd5(md5).getRefCount();
          //只能查找>0的！不合理！
             if(refcount==1){
                //完全没有文件,删磁盘文件，最后一个文件
                String path=mapper.selectFileById(id0).getPath();
                Files.delete(Path.of(path));
                id0s.add(id0);
                id0s.add(id);
                mapper.deletelink(id0);
            }
            //有文件
            else if(refcount>1){
                if(id!=id0)
                id0s.add(id);
            }
            uploadmapper.updateRefCount2(id0);
        }
        Long sum=0L;
        for (Integer id: id0s) {
            File f=mapper.selectFileById(id);
            sum+=f.getFileSize();
        }
        if(!id0s.isEmpty()){
            User u=uploadmapper.selectUserById(CurrentHolder.get());
            u.setVolume(u.getVolume()-sum);
            uploadmapper.updateVolume(u);
            mapper.deleteFile(new ArrayList<>(id0s));//删除记录
        }
    }

    @Override
    public shareinfo share(Integer fileId) {
        if(mapper.selectFileById(fileId).getUserId()!=CurrentHolder.get()){
            throw new RuntimeException("权限错误");
        }
        if(mapper.selectFileById(fileId).getIsFolder()==1){
            throw new RuntimeException("文件夹不能分享");
        }
        String link= UUID.randomUUID().toString().substring(0,8);
        LocalDateTime time=LocalDateTime.now();
        LocalDateTime time1=time.plusDays(7);
        //查找md5
        String md5=mapper.selectFileById(fileId).getFileName();
        //根据MD5确定原始文件id
        Integer fileId0=uploadmapper.selectByMd5(md5).getId();
        mapper.share(fileId0,link,time1);
        return new shareinfo(link,fileId0,time1);
    }

    @Override
    public File sharelinkinfo(String link) {
        shareinfo linkinfo = mapper.findlink(link);
        if(linkinfo!=null&&linkinfo.getLoseTime().isAfter(LocalDateTime.now())){
            return mapper.selectFileById(linkinfo.getFileId());
        }
        return null;
    }

    @Override
    public Integer store(String link,Integer parentId) {
        if(parentId!=0&&mapper.selectFileById(parentId).getUserId()!=CurrentHolder.get()){
            throw new RuntimeException("权限错误");
        }
        shareinfo linkinfo=mapper.findlink(link);
        if(linkinfo==null)return -2;//链接错误或者文件已经被删除
        if(linkinfo.getLoseTime().isBefore(LocalDateTime.now()))return 1;
        //连接超时
        Integer fileId=linkinfo.getFileId();
        File existFile=mapper.selectFileById(fileId);
        Integer userId=CurrentHolder.get();
        User user=uploadmapper.selectUserById(userId);
        if(user.getVolume()+existFile.getFileSize()>volume) {
            return -3;
        }
        if(existFile==null)return -2;
        String md5=existFile.getFileName();
        File fnow=mapper.selectFileById(fileId);
        if(fnow.getUserId().equals(CurrentHolder.get())&&fnow.getFileName().equals(md5)){
            return -1;//文件已存在
        }
        //File  = uploadmapper.selectByMd5(md5);
        uploadmapper.updateRefCount(existFile.getId());
        uploadmapper.insertRecord(new File(md5, existFile.getOriginalFileName(), CurrentHolder.get(),existFile.getFileSize(), existFile.getPath(), LocalDateTime.now(),0,parentId,0));
        return 0;
    }

    @Override
    public void download(Integer fileId, HttpServletResponse response) throws IOException {
        File file=mapper.selectFileById(fileId);
        if(file.getUserId()==CurrentHolder.get()){
            //有权限
            //先给文件引用数加1，防止被删除
            uploadmapper.updateRefCount(fileId);
            //下载逻辑
            Path path= Path.of(file.getPath());//获取文件路径对象
            if(!Files.exists(path))throw new FileNotFoundException("文件丢失");//文件丢失
            try {
                //设置包装信息，告诉浏览器下载文件
                response.setContentType("application/octet-stream");//设置响应头，告诉浏览器下载文件
                String fileName = URLEncoder.encode(file.getOriginalFileName(), "UTF-8").replaceAll("\\+", "%20");//解决中文乱码
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);//强制浏览器以附件形式下载，并且指定文件名
                response.setContentLength(file.getFileSize().intValue());//提前告诉浏览器文件大小，使浏览器出现下载进度条
                //板运数据
                Files.copy(path,response.getOutputStream());
            }catch (IOException e){
                throw new RuntimeException(e);
            }finally{
                uploadmapper.updateRefCount2(fileId);//无论是否成功，将文件引用数减1
            }
        }
        else throw new RuntimeException("权限错误");
    }

    @Override
    public void preview(Integer fileId, HttpServletResponse response) {
        List<String> list = Arrays.asList(extName);
        File file=mapper.selectFileById(fileId);
        String originalFileName=file.getOriginalFileName();
        if (!originalFileName.contains(".")) {
            throw new RuntimeException("无法识别文件格式，请下载后查看");
        }
        File originalFile=uploadmapper.selectByMd5(file.getFileName());
        String extname=originalFileName.substring(originalFileName.lastIndexOf(".")+1);
        if(file.getUserId()==CurrentHolder.get()||mapper.selectlinkByfileId(originalFile.getId())!=null){
            //有权限
            //先给文件引用数加1，防止被删除
            uploadmapper.updateRefCount(fileId);
            //下载逻辑
            Path path= Path.of(file.getPath());//获取文件路径对象
            if(!Files.exists(path))throw new RuntimeException("文件丢失");//文件丢失
            try {
                //设置包装信息，告诉浏览器下载文件
                String contentType;
                switch (extname.toLowerCase()) {
                    case "jpg": case "jpeg": contentType = "image/jpeg"; break;
                    case "png": contentType = "image/png"; break;
                    case "gif": contentType = "image/gif"; break;
                    case "bmp": contentType = "image/bmp"; break;
                    case "webp": contentType = "image/webp"; break;
                    case "pdf": contentType = "application/pdf"; break;
                    case "txt": contentType = "text/plain"; break;
                    case "md": contentType = "text/markdown"; break;
                    default: 
                        if(list.contains(extname)) {
                            contentType = "application/octet-stream";
                        } else {
                            throw new RuntimeException("该格式暂不支持在线预览");
                        }
                }
                response.setContentType(contentType);
                String fileName = URLEncoder.encode(file.getOriginalFileName(), "UTF-8").replaceAll("\\+", "%20");//解决中文乱码
                response.setHeader("Content-Disposition", "inline; filename=" + fileName);//强制浏览器以附件形式下载，并且指定文件名
                response.setContentLength(file.getFileSize().intValue());//提前告诉浏览器文件大小，使浏览器出现下载进度条
                //板运数据
                Files.copy(path,response.getOutputStream());
            }catch (IOException e){
                throw new RuntimeException(e);
            }finally{
                uploadmapper.updateRefCount2(fileId);//无论是否成功，将文件引用数减1
            }
        }
        else throw new RuntimeException("权限错误");
    }

    @Override
    public void mkdir(String originalFileName,Integer parentId) {
        //这里改了ref_count，因为我在delete那里写死了，所以那里要改
        File f=new File("",originalFileName,CurrentHolder.get(),null,path+originalFileName,LocalDateTime.now(),0,parentId,1);
        uploadmapper.insertRecord(f);
    }




//写一个递归函数，将所有子文件夹的id0都加入set中

    public  Set<File> toOnly(ArrayList<File> list,Set<File> onlyid0s){
        for (File file : list) {
            if(file.getIsFolder()==0){
                onlyid0s.add(file);
            }
            else {
                List<File> files =mapper.selectByParentId(file.getId());
                toOnly((ArrayList<File>) files, onlyid0s);
            }
        }
        return onlyid0s;
    }
}
