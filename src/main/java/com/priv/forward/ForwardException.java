package com.priv.forward;

/**
 * 转发相关异常类
 */
public final class ForwardException extends RuntimeException{

    public ForwardException(String message) {
        super(message);
    }

    public ForwardException(String message, Throwable cause) {
        super(message, cause, true, false);
    }

}
