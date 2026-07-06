package com.xushu.webtable.interceptor;

import com.xushu.webtable.utils.CurrentHolder;
import com.xushu.webtable.utils.jwtUtils;
import com.xushu.webtable.common.Const;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class logininterceptor implements HandlerInterceptor {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        /**
         * 设置追踪id
         */
        //  一定放在 preHandle 的最前面，这样即使登录校验失败
        //  （比如 token 为空直接返回 false 或抛异常），
        //  后续的 GlobalExceptionHandler 在构造 Result 时也能拿到 TraceId。
        String traceId = UUID.randomUUID().toString().replace("-","");
        MDC.put(Const.MDC_TRACE_ID, traceId);
        // 在您的拦截器 preHandle 方法第一行加上：
        String uri = request.getRequestURI();
        // 放行 OPTIONS 预检请求（跨域支持）以及公开资源路径
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) 
                || uri.contains("/login") 
                || uri.contains("/regi") 
                || uri.equals("/") 
                || uri.endsWith("/index.html") 
                || uri.endsWith("/favicon.ico")) {
            if (uri.contains("/login") || uri.contains("/regi")) {
                CurrentHolder.setRole(Const.ROLE_GUEST);  // 登录/注册接口需要 GUEST 角色
            }
            return true;
        }
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }
        if(token==null||token.isEmpty()){
            token=request.getParameter("token");
        }
        if(token==null||token.isEmpty()){
            log.info("用户未登录！");
            //response.setStatus(Const.HTTP_UNAUTHORIZED);
            CurrentHolder.setRole(Const.ROLE_GUEST);//设置角色为游客
            return true;
        }
        try {
            Map<String, Object> map = jwtUtils.parsejwt(token);//尝试解析令牌
            CurrentHolder.set((Integer)map.get(Const.JWT_CLAIM_ID));
            CurrentHolder.setRole((Integer)map.get(Const.JWT_CLAIM_ROLE));
           Boolean key= redisTemplate.expire(Const.REDIS_LOGIN_INFO_KEY_PREFIX+CurrentHolder.get()
                    , Duration.ofHours(Const.REDIS_TOKEN_LOSE_HOURS));
           if(!key){
               log.info("用户登录信息已过期！");
               response.setStatus(Const.HTTP_UNAUTHORIZED);
               return false;
           }
            return true;
        }catch (Exception e){
            //令牌解析不出来就是错的！
            log.info("请求错误");
            response.setStatus(Const.HTTP_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        CurrentHolder.remove();
        MDC.clear();
    }

}
