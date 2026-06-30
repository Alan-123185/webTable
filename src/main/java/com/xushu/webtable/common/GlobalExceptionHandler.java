package com.xushu.webtable.common;

import com.xushu.webtable.ManageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 拦截自定义业务异常
     */
    @ExceptionHandler(ManageException.class)
    public Result<Void> handleBusinessException(ManageException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(),e.getMessage());
    }
    /**
     * 拦截参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 处理参数校验异常的方法
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 获取所有字段错误信息，格式化为"字段名:错误信息"并用逗号连接，若无错误则默认提示
        String msg = e.getBindingResult().getFieldErrors().stream().map(err ->
                err.getField() + ":" + err.getDefaultMessage())
                .reduce((a, b) -> a + "," + b)
                .orElse("参数校验失败");
        // 记录参数校验失败的警告日志
        log.warn("参数校验失败: {}", msg);
        return Result.error(400,msg);
    }
    /**
     * 兜底：拦截所有未处理的异常
     * 数据库挂了、NPE、IO异常等都会到这里
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error(500,"系统异常: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
    }
}
