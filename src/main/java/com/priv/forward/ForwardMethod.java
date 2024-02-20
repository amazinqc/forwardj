package com.priv.forward;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.priv.forward.local.ForwardCrossThread;
import com.priv.forward.mock.LogicException;
import com.priv.forward.mock.struct.ForwardData;
import com.priv.forward.mock.struct.ListByteArray;
import com.priv.forward.rpc.converter.Converter;
import com.priv.forward.rpc.converter.MethodArgs;

public class ForwardMethod {

    private final Object target;

    private final Method method;

    private final boolean async;

    private final Forward[] forwards;

    public ForwardMethod(Object target, Method method) {
        this.target = target;
        this.method = method;
        this.async = isAsync(method);
        this.forwards = method.getAnnotationsByType(Forward.class);
    }

    /**
     * 是否异步返回
     */
    public static boolean isAsync(Method method) {
        return method.getReturnType().isAssignableFrom(CompletableFuture.class);
    }

    /**
     * 是否静默反馈
     */
    public static boolean isFeedbackSilence(Forward forward) {
        return forward != null && !forward.callback();
    }

    /**
     * 直接反射调用
     */
    public static Object invoke(Object target, Method method, Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new ForwardException("调用错误", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ForwardException || cause instanceof LogicException) {
                throw (RuntimeException) cause;
            }
            throw new ForwardException("调用错误", cause);  // if null
        }
    }

    public String getForwardName() {
        return method.getDeclaringClass().getSimpleName();
    }

    public String getMethodName() {
        return method.getName();
    }

    public boolean isAsync() {
        return async;
    }

    public Type getGenericReturnType() {
        return method.getGenericReturnType();
    }

    /**
     * 获取指定转发组合层级
     */
    private int getForwardIndex(Forward forward) {
        int i = 0;
        for (; i < forwards.length; ++i) {
            if (forwards[i] == forward) {
                break;
            }
        }
        return i;
    }

    public ForwardData buildForwardData(Forward forward, Object[] args) {
        ListByteArray argsBytes = MethodArgs.buildProtobuf(method.getParameters(), args);
        return ForwardData.newBuilder().setArgs(argsBytes).setIndex(getForwardIndex(forward)).build();
    }

    /**
     * 获取指定层级的转发策略
     */
    public Forward getForward(int forwardIndex) {
        if (forwardIndex >= forwards.length) {
            return null;
        }
        return forwards[forwardIndex];
    }

    /**
     * 直接调用
     */
    public Object invokeNow(Object[] args) {
        return invoke(target, method, args);
    }

    /**
     * 转发调用
     */
    private Object invokeForward(Forward forward, Object[] args) {
        return ForwardProxy.getForwardHandle(forward).forward(forward, this, args);
    }

    /**
     * 首次优先调用，按复合组合次序
     */
    public Object invokePriority(Object[] args) {
        // invoke priority forward
        return invokeForward(getForward(0), args);
    }

    /**
     * 邻次调用，处理下一个转发策略，一直到最后直接调用
     */
    public Object invokeNext(Forward forward, Object[] args) {
        forward = getForward(getForwardIndex(forward) + 1);
        return invokeForward(forward, args);
    }

    /**
     * 默认处理跨服本地调用，自动转发跨服处理线程（统一将跨服事件放在跨服线程处理）
     */
    public Object invokeCross(Forward forward, Object[] args) {
        return ForwardProxy.getForwardHandle(ForwardCrossThread.class).forward(forward, this, args);
    }

    /**
     * RPC调用，处理远端请求
     */
    public CompletableFuture<ByteString> invokeRpc(ByteString bytes) {
        ForwardData forwardData;
        Object[] args;
        try {
            forwardData = ForwardData.parseFrom(bytes);
            args = MethodArgs.buildObjectArgs(method.getParameters(), forwardData.getArgs());
        } catch (InvalidProtocolBufferException e) {
            throw new LogicException("数据错误");
        }
        int frowardIndex = forwardData.getIndex();
        Forward forward = getForward(frowardIndex + 1);
        Object returned = invokeForward(forward, args);
        if (isFeedbackSilence(getForward(frowardIndex))) {
            return null;
        }
        ForwardFuture<ByteString> future = new ForwardFuture<>();
        if (async) {
            CompletableFuture<?> f = (CompletableFuture<?>) returned;
            // assert CompletableFuture<T> 后续升级增强功能
            Type returnType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            f.thenAccept(val -> future.complete(MethodArgs.buildByteString(returnType, val, getReturnConverter()))).exceptionally(throwable -> {
                future.completeExceptionally(throwable);
                return null;
            });
        } else {
            future.obtrudeValue(MethodArgs.buildByteString(method.getGenericReturnType(), returned, getReturnConverter()));
        }
        return future;
    }

    public Converter getReturnConverter() {
        return method.getAnnotatedReturnType().getAnnotation(Converter.class);
    }
}
