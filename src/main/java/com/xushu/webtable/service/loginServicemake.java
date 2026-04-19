package com.xushu.webtable.service;

import com.xushu.webtable.common.User;
import com.xushu.webtable.common.LoginResult;
import com.xushu.webtable.mapper.loginMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class loginServicemake implements loginService {
    @Autowired
    private loginMapper loginmapper;

    @Override
    public LoginResult login(User user) {
        User u = loginmapper.findName(user.getUserName());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (u == null) {
            return new LoginResult(1, null); // 用户不存在
        }
        if (encoder.matches(user.getPassword(), u.getPassword())) {
            return new LoginResult(0, u);
        }
        return new LoginResult(2, null);  // 密码错误
    }
}
