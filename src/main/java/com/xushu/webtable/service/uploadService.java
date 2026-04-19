package com.xushu.webtable.service;


import com.xushu.webtable.common.File;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface uploadService {
    public void upload(File file,byte[] bytes, String md5, String diskPath) throws IOException;
}
