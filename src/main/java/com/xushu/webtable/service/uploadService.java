package com.xushu.webtable.service;


import com.xushu.webtable.common.CheckRequest;
import com.xushu.webtable.common.CheckResult;
import com.xushu.webtable.common.File;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface uploadService {

    CheckResult check(CheckRequest checkRequest);
    public void merger(CheckResult checkResult, Integer parentId);
    public void chunk(MultipartFile file, String diskPath, Integer index);
}
