package com.xushu.webtable.service.maker;

import cn.hutool.core.util.RandomUtil;
import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.RegisterRequest;
import com.xushu.webtable.common.User;
import com.xushu.webtable.mapper.loginMapper;
import com.xushu.webtable.mapper.regiMapper;
import com.xushu.webtable.service.regiService;
import com.xushu.webtable.utils.MailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.Duration;

@Slf4j
@Service
public class regiServicemake implements regiService {
    @Autowired
    private MailUtils mailUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Value("${path.pathhead}")
    private String pathhead;
    @Autowired
    private regiMapper regimapper;
    @Autowired
    private loginMapper loginMapper;
    @Override
    public Boolean getCode(RegisterRequest registerRequest) {
        User u1=loginMapper.findName(registerRequest.getUserName());
        User u2=loginMapper.findEmail(registerRequest.getEmail());
        if(u1!=null||u2!=null){
            throw new ManageException("用户名或邮箱已存在！");
        }
        //密码加密
        User user=new User();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(registerRequest.getPassword()));
        user.setUserName(registerRequest.getUserName());
        // 生成 6 位数字验证码
        String code = RandomUtil.randomNumbers(6);
        log.info("生成验证码：{}", code);
        stringRedisTemplate.opsForValue().set(Const.REDIS_USER_CODE_KEY_PREFIX+user.getUserName(), code, Duration.ofMinutes(Const.REDIS_CODE_TTL_MINUTES));//缓存验证码
        //调用工具发送到邮箱
        mailUtils.sendMail(registerRequest.getEmail(), code);
        return true;
    }

    @Override
    public Integer Register(RegisterRequest registerRequest,String code) {
        String redisCode = stringRedisTemplate.opsForValue().get(Const.REDIS_USER_CODE_KEY_PREFIX+registerRequest.getUserName());
        if(redisCode==null){
            return Const.REGISTER_CODE_EXPIRED;
        }
        if(code.equals(redisCode)){
            User user=new User(registerRequest.getUserName(),
                    new BCryptPasswordEncoder().encode(registerRequest.getPassword()),
                    registerRequest.getEmail(),
                    Const.ROLE_USER);
            regimapper.register(user);
            return Const.REGISTER_SUCCESS;
        }
        return Const.REGISTER_CODE_WRONG;
    }
}
