package com.priv.forward.mock;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import com.priv.forward.mock.net.CallData;
import com.priv.forward.mock.net.Message;
import com.priv.forward.mock.net.Session;
import com.priv.forward.mock.struct.ForwardData;

/**
 * 统一仿照工具
 */
public class MockUtil {

    public static final Object[] EMPTY_ARRAY = new Object[0];

    /**
     * 实例缓存
     */
    private static final ConcurrentHashMap<Class<?>, Object> instanceCaches = new ConcurrentHashMap<>();

    public static <T> T getInstance(Class<T> clazz) {
        Object instance = instanceCaches.get(clazz);
        if (instance == null) {
            instance = instanceCaches.computeIfAbsent(clazz, MockUtil::newInstance);
        }
        //noinspection unchecked
        return (T) instance;
    }

    private static <T> T newInstance(Class<T> clz) {
        try {
            return clz.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new IllegalStateException(clz.getName(), e);
        }
    }

    public static int getCentralServerId() {
        return 1;
    }

    public static Class<?> forName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * @return 线程粘滞 执行器（闭包）
     */
    public static Executor sticky() {
        Thread thread = Thread.currentThread();
        if (thread instanceof LogicThread) {
            LogicThread serverThread = (LogicThread) thread;
            return serverThread::summit;
        }
        return Runnable::run;
    }

    public static Throwable decodeThrowable(Throwable throwable) {
        return throwable instanceof CompletionException ? throwable.getCause() : throwable;
    }

    public static int getUserSid(long ignoredUid) {
        return 0;
    }

    public static int getServerInstanceId() {
        return 0;
    }

    public static Message buildFromProto(ForwardData forwardData, String forwardName, String methodName) {
        return Message.newBuilder().setMessageId(0).setMessageType(Message.MessageType.REQUEST).setCall(
                CallData.newBuilder().setForwardName(forwardName).setMethodName(methodName).setData(forwardData.toByteString()).build()
        ).build();
    }

    public static Session getTargetServer(int ignoredServerId) {
        return new Session();
    }
}
