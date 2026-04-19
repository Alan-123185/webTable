package com.xushu.webtable.service;

import com.xushu.webtable.common.File;
import com.xushu.webtable.common.User;
import com.xushu.webtable.mapper.uploadMapper;
import com.xushu.webtable.utils.CurrentHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class uploadServicemake implements uploadService {
    
    @Autowired
    private uploadMapper mapper;
    @Value("${user.volume}")
    private Long volume;
    
    @Override
    public void upload(File file, byte[] bytes, String md5, String diskPath) throws IOException {
        // 1. 根据 MD5 查询是否已存在相同文件
        Integer userId = CurrentHolder.get();
        User user=mapper.selectUserById(userId);
        if(user.getVolume()+file.getFileSize()>volume){
            throw new IOException("剩余空间不足");
        }
        else {
            user.setVolume(user.getVolume() + file.getFileSize());
            mapper.updateVolume(user);
        }
        File existFile = mapper.selectByMd5(md5);
        
        if (existFile != null) {
            // 秒传：文件已存在，只插入数据库记录，不保存文件
            file.setRefCount(0);//秒传为0
            mapper.insertRecord(file);
            mapper.updateRefCount(existFile.getId());
        } else {
            // 首次上传：保存文件到磁盘
            file.setRefCount(1);//首次上传为1
            Path path = Paths.get(diskPath);
            //Files.createDirectories(path.getParent()); // 确保目录存在
            try {
                //尝试保存文件
                Files.write(path,bytes);
                // 插入数据库记录
                mapper.insertRecord(file);
            }catch(DuplicateKeyException e){
                //重复导致保存失败,这是可能出现的并发问题
                mapper.updateRefCount(file.getId());
            }
        }
    }
}
