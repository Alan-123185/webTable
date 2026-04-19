package com.xushu.webtable.service;

import com.xushu.webtable.common.User;
import com.xushu.webtable.mapper.regiMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.io.File;

@Service
public class regiServicemake implements regiService{
    @Value("${path.pathhead}")
    private String pathhead;
    @Autowired
    private regiMapper regimapper;
    @Override
    public Boolean register(User user) {
        //密码加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        user.setVolume(0L);
        Integer ret = regimapper.register(user);
        if(ret==0)return false;
        return true;
    }

//    @Override
//    public void makedir(User user) {
//        File nf=new File(pathhead+user.getId()+"/myResource");
//        nf.mkdirs();
//    }
}
