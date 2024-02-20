package com.priv.forward;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.priv.forward.local.ForwardSelf;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(Forward.List.class)
public @interface Forward {

    /**
     * 转发策略，默认不转发
     */
    Class<? extends ForwardHandle> value() default ForwardSelf.class;

    /**
     * 失败时重试次数，默认不重试
     */
    @Deprecated
    int repeatOnFail() default 0;

    /**
     * 超时/毫秒，默认没有超时
     */
    @Deprecated
    int timeout() default 0;

    /**
     * 需要等待响应回调，默认都需要等待目标执行结束并返回
     */
    boolean callback() default true;

    @Inherited
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {
        @SuppressWarnings("unused") Forward[] value();
    }
}
