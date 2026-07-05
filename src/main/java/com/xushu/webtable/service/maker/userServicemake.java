package com.xushu.webtable.service.maker;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.selectUserBean;
import com.xushu.webtable.common.selectUserResult;
import com.xushu.webtable.mapper.userMapper;
import com.xushu.webtable.service.loginService;
import com.xushu.webtable.service.userService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
public class userServicemake implements userService {
      @Autowired
      private RedisTemplate<String,Object> redisTemplate;

      @Autowired
      private loginService loginservice;

      @Autowired
      private userMapper usermapper;


    @Override
    public void unban(Integer userId) {
        usermapper.unbanforstatus(userId);
        boolean pan=redisTemplate.delete(Const.REDIS_USER_BANNED + ":" + userId);
        if(!pan)usermapper.unban(userId);
    }

    @Override
    public void ban(Integer userId,int level,String reason) {
            loginservice.logout();
            usermapper.ban(userId);
            switch ( level) {
                case Const.BAN_LEVEL_1:
                    redisTemplate.opsForValue().set(Const.REDIS_USER_BANNED + ":" + userId, reason);
                    redisTemplate.expire(Const.REDIS_USER_BANNED + ":" + userId, Duration.ofDays(Const.BAN_LEVEL_1_DAYS));
                    break;
                case Const.BAN_LEVEL_2:
                    redisTemplate.opsForValue().set(Const.REDIS_USER_BANNED + ":" + userId,reason);
                    redisTemplate.opsForValue().set(Const.REDIS_USER_BANNED + ":" + userId, Duration.ofDays(Const.BAN_LEVEL_2_DAYS));
                    break;
                case Const.BAN_LEVEL_3:
                    //永久封禁直接进入数据库
                    usermapper.ban(userId,reason);
                default:
                    throw new ManageException(500, "ban level error");
            }
    }

    @Override
    public selectUserResult selectUserByName(String userName,int page,int number) {
        PageHelper.startPage(page, number);
        List<selectUserBean> userbeans = usermapper.selectUserByName(userName);
        Page<selectUserBean> p = (Page<selectUserBean>) userbeans;
        return new selectUserResult(p.getResult(),p.getTotal());
    }

    @Override
    public selectUserResult selectBannedByName(String userName,int page,int number) {
        PageHelper.startPage(page, number);
        List<selectUserBean> userbeans = usermapper.selectBannedByName(userName);
        Page<selectUserBean> p = (Page<selectUserBean>) userbeans;
        return new selectUserResult(p.getResult(),p.getTotal());
    }
}
