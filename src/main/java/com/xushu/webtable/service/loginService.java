package com.xushu.webtable.service;

import com.xushu.webtable.common.LoginRequest;
import com.xushu.webtable.common.LoginResult;
import com.xushu.webtable.common.Result;
import com.xushu.webtable.common.User;


public interface loginService {
    LoginResult loginpan(LoginRequest loginRequest);
    Long getVolume(Integer userId);
    Result login(User user,Integer ret);
    Result logout();
}
