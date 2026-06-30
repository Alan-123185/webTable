package com.xushu.webtable.service;

import com.xushu.webtable.common.File;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface recycleBinService {
    public void delete(List<Integer> fileIds);
    void goback(List<Integer> fileIds);
    List<File> getRecycleFiles(Integer userId);

}
