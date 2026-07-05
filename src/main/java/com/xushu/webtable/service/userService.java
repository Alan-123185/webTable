package com.xushu.webtable.service;

import com.xushu.webtable.common.selectUserBean;
import com.xushu.webtable.common.selectUserResult;

import java.util.List;

public interface userService {

     void unban(Integer userId);

     void ban(Integer userId,int level,String reason);

     selectUserResult selectUserByName(String userName, int page, int number);

     selectUserResult selectBannedByName(String userName,int page,int number);


}
