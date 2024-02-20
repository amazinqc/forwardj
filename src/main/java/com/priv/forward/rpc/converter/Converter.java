package com.priv.forward.rpc.converter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数序列化扩展
 */
@Inherited
@Documented
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Converter {

    /**
     * 自定义转换器，默认使用 {@link MethodArgs} 枚举子类处理
     */
    Class<? extends ProtobufConverter> value() default DefaultConverter.class;
}
