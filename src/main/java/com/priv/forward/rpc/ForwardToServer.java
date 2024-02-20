package com.priv.forward.rpc;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.priv.forward.Forward;
import com.priv.forward.ForwardException;
import com.priv.forward.ForwardHandle;
import com.priv.forward.ForwardMethod;
import com.priv.forward.mock.LogicException;
import com.priv.forward.mock.MockUtil;
import com.priv.forward.mock.struct.ForwardData;
import com.priv.forward.rpc.converter.MethodArgs;

/**
 * 转发到服务器
 */
public class ForwardToServer implements ForwardHandle {

    @Override
    public Object forward(Forward forward, ForwardMethod method, Object[] args) throws LogicException, ForwardException {
        int sid = getServerId(args);
        if (sid == MockUtil.getServerInstanceId()) {
            return method.invokeCross(forward, args);
        }
        ForwardData forwardData = method.buildForwardData(forward, args);
        if (ForwardMethod.isFeedbackSilence(forward)) {
            ServerRpcManager.callRpcNoBack(sid, method.getForwardName(), method.getMethodName(), forwardData);
            return null;
        }
        RpcFuture rpcFuture = ServerRpcManager.callRpcAsync(sid, method.getForwardName(), method.getMethodName(), forwardData);
        if (method.isAsync()) {   // 异步代理
            // assert CompletableFuture<T>
            Type returnType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            // 异步RpcFuture需要设置超时时间，防止异常无法销毁
            return rpcFuture.orTimeout().thenApplyAsync(returned -> {
                try {
                    return MethodArgs.buildObject(returnType, returned, method.getReturnConverter());
                } catch (InvalidProtocolBufferException e) {
                    throw new LogicException("数据错误");
                }
            }, MockUtil.sticky());
        }
        ByteString returned = rpcFuture.get();
        try {
            return MethodArgs.buildObject(method.getGenericReturnType(), returned, method.getReturnConverter());
        } catch (InvalidProtocolBufferException e) {
            throw new LogicException("数据错误");
        }
    }

    protected int getServerId(Object[] args) {
        if (args.length < 1 || (!(args[0] instanceof Integer))) {
            throw new ForwardException("缺少sid转发参数");
        }
        return (int) args[0];
    }
}
