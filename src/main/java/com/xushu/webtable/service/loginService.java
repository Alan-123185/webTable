package com.xushu.webtable.service;

import com.xushu.webtable.common.LoginRequest;
import com.xushu.webtable.common.LoginResult;


public interface loginService {
    public LoginResult login(LoginRequest loginRequest);
}
