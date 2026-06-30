package com.xushu.webtable.Aspect;

import com.xushu.webtable.ManageException;
import com.xushu.webtable.anno.Role;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.utils.CurrentHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class RoleAspect {
    /**
     * 环绕通知：拦截带有@Role注解的方法，检查用户角色是否满足权限要求
     *
     * @param joinPoint 切点对象
     * @return 目标方法的返回值
     * @throws Throwable 当目标方法执行异常或权限不足时抛出
     */
    @Around("@annotation(role)")
    public Object around(ProceedingJoinPoint joinPoint,Role role ) throws Throwable {
        int currole= CurrentHolder.getRole();
        int[] roles= role.value();
        for (int i : roles) {
            if(currole==i)
                return joinPoint.proceed();
        }
        throw new ManageException(Const.HTTP_FORBIDDEN,"用户角色 '" + currole + "' 不满足权限要求 '" + Arrays.toString(roles) + "'");
    }
}
