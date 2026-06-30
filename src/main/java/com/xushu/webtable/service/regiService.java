package com.xushu.webtable.service;

import com.xushu.webtable.common.RegisterRequest;
import com.xushu.webtable.common.User;

public interface regiService {
    public Integer Register(RegisterRequest registerRequest,String code);
    public Boolean getCode(RegisterRequest registerRequest);
   // public void makedir(User user);
}
