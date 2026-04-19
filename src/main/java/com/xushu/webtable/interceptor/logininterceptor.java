package com.xushu.webtable.interceptor;

import com.xushu.webtable.utils.CurrentHolder;
import com.xushu.webtable.utils.jwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Component
@Slf4j
public class logininterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在您的拦截器 preHandle 方法第一行加上：
        String uri = request.getRequestURI();
        if ("OPTIONS".equals(request.getMethod()) || uri.contains("/login") || uri.contains("/regi") ||
                uri.equals("/") || uri.equals("/index.html") || uri.equals("/favicon.ico")) {
            return true;
        }
        String str=request.getRequestURI();
        if(str.contains("/login") || str.contains("/regi") ){
            log.info("放行: {}", str);
            return true;
        }
        String token = request.getHeader("token");
        if(token==null||token.isEmpty()){
            token=request.getParameter("token");
        }
        if(token==null||token.isEmpty()){
            log.info("用户未登录！");
            response.setStatus(401);
            return false;
        }
        try {
            Map<String, Object> map = jwtUtils.parsejwt(token);
            CurrentHolder.set((Integer)map.get("id"));
            return true;
        }catch (Exception e){
            //令牌解析不出来就是错的！
            log.info("用户名或密码错误！");
            response.setStatus(401);
            return false;
        }
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
       CurrentHolder.remove();
    }
}
