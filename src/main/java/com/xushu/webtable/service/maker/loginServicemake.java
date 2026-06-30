package com.xushu.webtable.service.maker;

import com.xushu.webtable.common.*;
import com.xushu.webtable.mapper.loginMapper;
import com.xushu.webtable.service.loginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class loginServicemake implements loginService {
    @Autowired
    private loginMapper loginmapper;

    @Override
    public LoginResult login(LoginRequest loginRequest) {
        User u = loginmapper.findName(loginRequest.getUserName());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (u == null) {
            return new LoginResult(Const.LOGIN_NOT_REGISTERED, null); // 用户不存在
        }
        if (encoder.matches(loginRequest.getPassword(), u.getPassword())) {
            return new LoginResult(Const.LOGIN_SUCCESS, u);
        }
        return new LoginResult(Const.LOGIN_PASSWORD_WRONG, null);  // 密码错误
    }
}
