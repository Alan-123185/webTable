package com.xushu.webtable;

import lombok.Getter;

/**
*全局异常处理
*自定义异常类
 *
*
*/

@Getter
public class ManageException extends RuntimeException{
    private Integer code;
    private String msg;
    public ManageException(String msg){
        super(msg);
        this.code=0;   // 默认为0
        this.msg=msg;
    }
    /**
     * 构造带有错误码、错误信息和异常原因的自定义异常
     * @param code 错误码
     * @param msg 错误信息
     */
    public ManageException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
