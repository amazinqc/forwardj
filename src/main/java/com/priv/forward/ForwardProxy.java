package com.priv.forward;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.priv.forward.local.ForwardSelf;
import com.priv.forward.mock.LogicException;
import com.priv.forward.mock.MockUtil;
import com.priv.forward.mock.net.CallData;

/**
 * 基于Java接口代理的转发（后续可以视使用情况替换为cglib代理）
 */
public class ForwardProxy implements InvocationHandler {

    /**
     * 代理收集
     */
    private static final Map<String, Map<String, ForwardMethod>> proxyMap = new HashMap<>();

    /**
     * 单例优化
     */
    private static final ForwardProxy INSTANCE = new ForwardProxy();

    private ForwardProxy() {
    }

    /**
     * 创建转发代理
     *
     * @param interfaceClass 代理接口
     * @param target         代理目标对象
     * @param <T>            接口类型
     * @return 代理实例
     * @apiNote 默认使用jdk动态代理
     */
    public static <T> T newProxy(Class<T> interfaceClass, T target) {
        parseProxyMethod(interfaceClass, target);
        @SuppressWarnings("unchecked") T proxy = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, INSTANCE);
        return proxy;
    }

    private static void parseProxyMethod(Class<?> clazz, Object target) {
        Map<String, ForwardMethod> methodMap = Arrays.stream(clazz.getMethods()).collect(Collectors.toMap(Method::getName, method -> new ForwardMethod(target, method)));
        proxyMap.put(clazz.getSimpleName(), methodMap);
    }

    public static ForwardHandle getForwardHandle(Forward forward) {
        Class<? extends ForwardHandle> forwardClass = forward == null ? ForwardSelf.class : forward.value();
        return getForwardHandle(forwardClass);
    }

    public static ForwardHandle getForwardHandle(Class<? extends ForwardHandle> forwardClass) {
        return MockUtil.getInstance(forwardClass);
    }

    public static ForwardMethod getForwardMethod(String forwardName, String methodName) {
        Map<String, ForwardMethod> methodMap = proxyMap.get(forwardName);
        if (methodMap != null) {
            ForwardMethod method = methodMap.get(methodName);
            if (method != null) {
                return method;
            }
        }
        throw new ForwardException("转发错误：" + forwardName + '@' + methodName);
    }

    public static ForwardMethod getForwardMethod(CallData call) {
        return getForwardMethod(call.getForwardName(), call.getMethodName());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String forwardName = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        ForwardMethod forwardMethod = getForwardMethod(forwardName, methodName);
        try {
            return forwardMethod.invokePriority(args == null ? MockUtil.EMPTY_ARRAY : args);
        } catch (LogicException e) {
            throw e;
        } catch (Throwable e) {
            throw new LogicException("操作失败");
        }
    }
}
