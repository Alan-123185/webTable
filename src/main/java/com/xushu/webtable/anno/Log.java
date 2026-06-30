package com.xushu.webtable.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志注解
 * 用于标记需要记录日志的方法
 */
@Target(ElementType.METHOD) // 作用于方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
public @interface Log {
    /**
     * 日志描述信息
     * @return 描述内容
     */
    String value() default "";

    /**
     * 操作类型（如：查询、新增、修改、删除等）
     * @return 操作类型
     */
    OperationType operationType() default  OperationType.OTHER;

    public enum OperationType {
        UPLOAD, DOWNLOAD, DELETE,STORE, RENAME,RESTORE, MOVE, LOGIN, REGISTER, SHARE, OTHER ,PREVIEW,LOGOUT,QUERY
    }
}
