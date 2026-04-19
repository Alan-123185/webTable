package com.xushu.webtable.service;

import com.xushu.webtable.common.File;
import com.xushu.webtable.common.selectFileBean;
import com.xushu.webtable.common.shareinfo;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface fileService {
     selectFileBean select(String originalFileName, Integer userId, int page, int number,Integer parentId);
     void delete(List<Integer> ids) throws IOException;
     shareinfo share(Integer fileId);
     Integer store(String link,Integer parentId);
     void download(Integer fileId, HttpServletResponse response) throws IOException;
     void preview(Integer fileId,HttpServletResponse response);
     void mkdir(String originalFileName,Integer parentId);
     File sharelinkinfo(String link);
}
