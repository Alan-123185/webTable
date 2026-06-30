package com.xushu.webtable.Aspect;

import com.xushu.webtable.anno.Log;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.OperationLog;
import com.xushu.webtable.service.logService;
import com.xushu.webtable.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面类
 * <p>
 * 该类使用AOP（面向切面编程）技术，拦截带有@Log注解的方法，
 * 自动记录方法的执行信息，包括操作用户、类名、方法名、参数、
 * 返回值、执行时间等，并将日志保存到数据库中。
 * </p>
 */
@Aspect
@Slf4j
@Component
public class PerformanceAspect {
    @Autowired
    private logService logservice;

    @Autowired
    private RedisTemplate<String,OperationLog> logredisTemplate;

    /**
     * 环绕通知：拦截带有@Log注解的方法，记录操作日志
     * <p>
     * 该方法会在目标方法执行前后进行拦截，收集方法的执行信息并保存到数据库中。
     * 对于登录接口（URI包含"/login"）不进行日志记录。
     * </p>
     *
     * @param joinPoint 切点对象
     * @return 目标方法的返回值
     * @throws Throwable 当目标方法执行异常时抛出
     */
    @Around("@annotation(com.xushu.webtable.anno.Log)")
    public Object LogMan(ProceedingJoinPoint joinPoint) throws Throwable {
        OperationLog log = new OperationLog();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        // 记录开始时间
        Object result;
        long start = System.currentTimeMillis();
        // 执行目标方法1
         try {
             result = joinPoint.proceed();
             log.setStatus(true);
         } catch (Throwable e) {
             log.setStatus(false);
             throw e;
         }finally {
             // 记录结束时间并计算耗时
             long end = System.currentTimeMillis();
             long duration = end - start;
             // 从@Log注解获取操作类型和描述
             Log logAnnotation = method.getAnnotation(Log.class);
             if (logAnnotation != null) {
                 log.setOperateType(logAnnotation.operationType().name());
                 log.setDescription(logAnnotation.value());
             }
             log.setCostMs(duration);
             log.setCreateTime(LocalDateTime.now());
             // 设置操作人ID
             log.setOperatorId(CurrentHolder.get());
             // 存入Redis队列，等待LogSaver消费后批量写入数据库
             logredisTemplate.opsForList().rightPush(Const.REDIS_LOG_INFO_KEY_PREFIX,log);
         }
         return result;
    }
}

