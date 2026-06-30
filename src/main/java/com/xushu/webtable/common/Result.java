package com.xushu.webtable.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import  org.slf4j.MDC;

/**
 * 修改result类，增强统一响应,改用泛型
 *添加时间戳
 * 添加traceId
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;
    private String traceId;
    public static <T> Result<T> success(){
        return success(null);
    }
    public static <T> Result<T> success(T data){
        String traceId = getTraceId();
        return new Result(Const.HTTP_SUCCESS,"success",data,System.currentTimeMillis(),traceId);
    }
    public static Result error(Integer code,String msg){
        return new Result(code,msg,null,System.currentTimeMillis(),getTraceId());
    }
    public static Result error(String msg){
        return error(Const.HTTP_INTERNAL_ERROR,msg);
    }

    // ---------- traceId 获取（从 MDC 中拿）----------
    private static String getTraceId() {
            // 从 MDC (Mapped Diagnostic Context) 中获取当前线程绑定的 traceId
            // MDC 通常由拦截器或过滤器在请求进入时设置，用于链路追踪
            Object traceIdObj = MDC.get(Const.MDC_TRACE_ID);
            return traceIdObj != null ? traceIdObj.toString() : null;
    }

}
