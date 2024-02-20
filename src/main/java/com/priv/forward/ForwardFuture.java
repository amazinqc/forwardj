package com.priv.forward;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import com.priv.forward.mock.LogicException;


/**
 * 异步转发的Future对象
 */
public class ForwardFuture<T> extends CompletableFuture<T> {
    private static final int TIMEOUT = 120;

    @Override
    public T get() throws LogicException, ForwardException {
        try {
            return super.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException e) {
            onTimeout();
            throw new ForwardException("执行超时", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ForwardException || cause instanceof LogicException) {
                throw (RuntimeException) cause;
            }
            throw new ForwardException("执行异常", cause);
        }
    }

    protected void onTimeout() {
    }

    /**
     * 如果在给定超时之前没有完成，则使用 {@link TimeoutException} 异常完成该 RpcFuture
     *
     * @return RpcFuture this对象
     */
    public ForwardFuture<T> orTimeout() {
        if (!super.isDone()) {
            super.whenComplete(new Canceller(Delayer.delay(new RpcTimeout(this))));
        }
        return this;
    }


    /**
     * 用于在超时时触发completeExceptionally
     *
     * @see extends JDK 1.9+
     */
    static final class RpcTimeout implements Runnable {
        final ForwardFuture<?> future;

        RpcTimeout(ForwardFuture<?> future) {
            this.future = future;
        }

        public void run() {
            if (!future.isDone()) {
                // 异步超时后，需要移除rpc缓存
                if (future.completeExceptionally(new TimeoutException())) {
                    future.onTimeout();
                }
            }
        }
    }

    /**
     * 用于取消不需要的超时操作
     *
     * @see extends JDK 1.9+
     */
    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> future;

        Canceller(Future<?> future) {
            this.future = future;
        }

        public void accept(Object ignore, Throwable ex) {
            if (null == ex && !future.isDone()) {
                future.cancel(false);
            }
        }
    }

    /**
     * 单例延迟调度器，仅用于启动和取消任务
     *
     * @see extends JDK 1.9+
     */
    static final class Delayer {
        static final ScheduledThreadPoolExecutor delayer;

        static {
            (delayer = new ScheduledThreadPoolExecutor(
                    1, new DaemonThreadFactory())).
                    setRemoveOnCancelPolicy(true);
        }

        static ScheduledFuture<?> delay(Runnable command) {
            return delayer.schedule(command, ForwardFuture.TIMEOUT, TimeUnit.SECONDS);
        }

        static final class DaemonThreadFactory implements ThreadFactory {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("ForwardFutureDelayScheduler");
                return t;
            }
        }
    }
}
