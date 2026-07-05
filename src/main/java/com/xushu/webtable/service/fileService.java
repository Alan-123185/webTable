package com.xushu.webtable.service;

import com.xushu.webtable.common.File;
import com.xushu.webtable.common.selectFileBean;
import com.xushu.webtable.common.shareinfo;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface fileService {
     selectFileBean select(String originalFileName, Integer userId, int page, int number,Integer parentId);
     void softdelete(List<Integer> ids);
     shareinfo share(Integer fileId);
     Integer store(String link,Integer parentId);
     void download(Integer fileId, HttpServletResponse response);
     void preview(Integer fileId,String link,HttpServletResponse response);
     void mkdir(String originalFileName,Integer parentId);
     File sharelinkinfo(String link);
     Integer rename(Integer fileId,String newName);
     void move(Integer fileId,Integer parentId);
     void batchDownload(List<Integer> fileIds, HttpServletResponse response);
     void forceDelete(List<Integer> fileIds);
}
