package com.xushu.webtable.service.maker;

import com.xushu.webtable.common.*;
import com.xushu.webtable.mapper.loginMapper;
import com.xushu.webtable.mapper.userMapper;
import com.xushu.webtable.service.loginService;
import com.xushu.webtable.utils.CurrentHolder;
import com.xushu.webtable.utils.jwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Slf4j
@Service
public class loginServicemake implements loginService {
    @Autowired
    private loginMapper loginmapper;
    @Autowired
    private userMapper usermapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginResult loginpan(LoginRequest loginRequest) {
        User u = loginmapper.findName(loginRequest.getUserName());
        if (u == null) {
            return new LoginResult(Const.LOGIN_NOT_REGISTERED, null); // 用户不存在
        }
        if(redisTemplate.hasKey(Const.REDIS_USER_BANNED+":"+u.getId()) || usermapper.findBannedUser(u.getId())!=0){
            return new LoginResult(Const.LOGIN_BANNED, u);
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(loginRequest.getPassword(), u.getPassword())) {
            return new LoginResult(Const.LOGIN_SUCCESS, u);
        }
        return new LoginResult(Const.LOGIN_PASSWORD_WRONG, null);  // 密码错误
    }


    @Override
    public Result login(User user,Integer res) {
        if (res == Const.LOGIN_SUCCESS) {
            log.info("用户{}登录成功！", user);
            String token = jwtUtils.makejwt(user.getUserName(), user.getId(), user.getRole());
            CurrentHolder.set(user.getId().intValue());
            logininfo info = new logininfo(user.getUserName(), user.getId(), token, user.getVolume(), user.getAllVolume());
            redisTemplate.opsForValue().set(Const.REDIS_LOGIN_INFO_KEY_PREFIX + user.getId(),
                    "1",
                    Duration.ofHours(Const.REDIS_TOKEN_LOSE_HOURS));
            return Result.success(info);
        }
        if (res == Const.LOGIN_NOT_REGISTERED) {
            log.info("{}未注册！", user);
            return Result.error("用户未注册");
        }
        if (res == Const.LOGIN_PASSWORD_WRONG) {
            log.info("用户名或密码错误！{}", user);
            return Result.error("用户名或密码错误");
        }
        if (res == Const.LOGIN_BANNED) {
            log.info("未知错误！{}", user);
            return Result.error("用户被封禁");
        }
        return Result.error("未知错误");
    }

    @Override
    public Result logout() {
        redisTemplate.delete(Const.REDIS_LOGIN_INFO_KEY_PREFIX+ CurrentHolder.get());
        return Result.success();
    }


    @Override
    public Long getVolume(Integer userId) {
        Long volume=loginmapper.getVolume(userId);
        redisTemplate.opsForValue().set(Const.REDIS_USER_VOLUME+":"+userId, volume);
        return volume;
    }
}
