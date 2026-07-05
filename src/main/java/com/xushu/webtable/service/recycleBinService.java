package com.xushu.webtable.service;

import com.xushu.webtable.common.File;

import java.util.List;
import java.util.Set;

public interface recycleBinService {
    void delete(List<Integer> fileIds);
    void goback(List<Integer> fileIds);
    List<File> getRecycleFiles(Integer userId);
}
