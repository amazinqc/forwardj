package com.priv.forward.local;

import java.util.concurrent.CompletableFuture;

import com.priv.forward.Forward;
import com.priv.forward.ForwardException;
import com.priv.forward.ForwardHandle;
import com.priv.forward.ForwardMethod;
import com.priv.forward.mock.MockUtil;
import com.priv.forward.mock.LogicException;
import com.priv.forward.mock.LogicThread;


/**
 * 线程转发
 */
public abstract class ForwardToThread implements ForwardHandle {
    @Override
    public Object forward(Forward forward, ForwardMethod method, Object[] args) throws LogicException, ForwardException {
        LogicThread thread = getTargetThread(args);
        if (thread == Thread.currentThread()) {
            return method.invokeNext(forward, args);
        }

        if (ForwardMethod.isFeedbackSilence(forward)) {
            thread.summit(() -> method.invokeNext(forward, args));
            return null;
        }

        LocalFuture future = new LocalFuture(() -> method.invokeNext(forward, args));
        thread.summit(future);
        if (method.isAsync()) {
            return future.thenComposeAsync(returned -> {
                // assert CompletableFuture<T>
                return (CompletableFuture<?>) returned;
            }, MockUtil.sticky());
        }
        return future.get();
    }

    protected abstract LogicThread getTargetThread(Object[] args);
}
