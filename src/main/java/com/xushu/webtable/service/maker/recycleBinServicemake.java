package com.xushu.webtable.service.maker;

import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.File;
import com.xushu.webtable.mapper.fileMapper;
import com.xushu.webtable.mapper.recycleBinMapper;
import com.xushu.webtable.mapper.uploadMapper;
import com.xushu.webtable.service.recycleBinService;
import com.xushu.webtable.utils.CurrentHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class recycleBinServicemake implements recycleBinService {
    @Autowired
    private uploadMapper uploadmapper;
    @Autowired
    private recycleBinMapper recycleBinMapper;
    @Autowired
    private fileMapper mapper;

    /**
     * 从回收站复原文件
     * refcount的更新是重点
     *
     * @param ids
     */
    @Override
    @Transactional
    public void goback(List<Integer> ids) {
        for (Integer id : ids) {
            File f = recycleBinMapper.selectFileInBin(id);
            String md5 = f.getFileName();
            if (uploadmapper.selectByMd5(md5) != null) {
                f.setRefCount(0);
                uploadmapper.updateRefCount(uploadmapper.selectByMd5(md5).getId());
            } else f.setRefCount(1);
            if (f.getUserId() != CurrentHolder.get()) {
                throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
            }
            if (recycleBinMapper.getrecycletime(id).isBefore(LocalDateTime.now())) {
                throw new ManageException(Const.HTTP_FORBIDDEN, "文件已过期");
            }
            if (f.getParentId() != 0 && mapper.selectFileById(f.getParentId()) == null)
                f.setParentId(0);
            uploadmapper.insertRecord(f);
        }
    }
    @Override
    public List<File> getRecycleFiles(Integer userId){
        return recycleBinMapper.getRecycleFiles(userId);
    }

    /**
     * 回收站删除
     *
     * @param ids 文件ID列表
     */
    @Override
    @Transactional
    public void delete(List<Integer> ids) {
        for (Integer id : ids) {
            File f = recycleBinMapper.selectFileInBin(id);
            if (f.getUserId() != CurrentHolder.get()) {
                throw new ManageException(Const.HTTP_FORBIDDEN, "权限错误");
            }
            int count = recycleBinMapper.binfilecount(f.getFileName());
            if (count == 1) {
                //删除磁盘文件
                try {
                    Files.delete(Path.of(f.getPath()));
                } catch (IOException e) {
                    throw new ManageException(Const.HTTP_NOT_FOUND, "文件不存在");
                }
            }
            recycleBinMapper.deletebinfile(id);
        }
    }

}
