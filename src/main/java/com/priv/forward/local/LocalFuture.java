package com.priv.forward.local;


import java.util.concurrent.Callable;

import com.priv.forward.ForwardFuture;

/**
 * 异步执行的Future对象
 */
public class LocalFuture extends ForwardFuture<Object> implements Runnable {

    private final Callable<Object> callable;

    public LocalFuture(Callable<Object> callable) {
        this.callable = callable;
    }

    /**
     * 本地策略取消超时设定
     */
    @Override
    public ForwardFuture<Object> orTimeout() {
        return this;
    }

    @Override
    public void run() {
        try {
            complete(callable.call());
        } catch (Exception e) {
            completeExceptionally(e);
        }
    }
}
