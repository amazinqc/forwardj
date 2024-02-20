package com.priv.forward.rpc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ByteString;
import com.priv.forward.ForwardException;
import com.priv.forward.ForwardMethod;
import com.priv.forward.ForwardProxy;
import com.priv.forward.mock.LogicException;
import com.priv.forward.mock.MockUtil;
import com.priv.forward.mock.net.CallData;
import com.priv.forward.mock.net.CallbackData;
import com.priv.forward.mock.net.Message;
import com.priv.forward.mock.net.Session;
import com.priv.forward.mock.struct.ForwardData;

public class ServerRpcManager {


    private static final Map<Integer, RpcFuture> futureMap = new ConcurrentHashMap<>();

    private static void logRpcRequest(int ignoredSid, int ignoredMid, String ignoredHandleName, String ignoredFuncName) {
    }

    private static void logRpcReceive(int ignoredMid, String ignoredHandleName, String ignoredFuncName) {
    }

    private static void logRpcResponse(int ignoredMid) {
    }

    private static void logRpcResponse(int ignoredMid, Throwable ignoredE) {
    }

    private static void logRpcResponse(int ignoredMid, String ignoredLogic) {
    }

    private static void logRpcCallback(int ignoredMid) {
    }

    private static void logRpcCallback(int ignoredMid, String ignoredError) {
    }

    /**
     * 处理rpc请求
     *
     * @param session rpc链接对象
     * @param msg     请求数据
     */
    public static void handleRpcRequest(Session session, Message msg) {
        CallbackData.Builder builder = CallbackData.newBuilder();
        CallData call = msg.getCall();
        int messageId = msg.getMessageId();
        logRpcReceive(messageId, call.getForwardName(), call.getMethodName());
        try {
            ForwardMethod forwardMethod = ForwardProxy.getForwardMethod(call);
            CompletableFuture<ByteString> asyncFuture = forwardMethod.invokeRpc(call.getData());
            if (asyncFuture == null) {
                return;
            }
            if (asyncFuture.isDone()) {
                builder.setData(asyncFuture.get());
                logRpcResponse(messageId);
            } else {
                asyncFuture.thenAccept(value -> {
                    builder.setData(value);
                    logRpcResponse(messageId);
                }).exceptionally(throwable -> {
                    throwable = MockUtil.decodeThrowable(throwable);
                    if (throwable instanceof LogicException) {
                        builder.setErr(1);
                        String message = throwable.getLocalizedMessage();
                        builder.setMessage(message);
                        logRpcResponse(messageId, message);
                    } else {
                        builder.setErr(2).setMessage("服务器错误");
                        logRpcResponse(messageId, throwable);
                    }
                    return null;
                }).whenComplete((unused, throwable) -> sendServerResponse(session, builder.build(), messageId));
                return;
            }
        } catch (LogicException e) {
            String message = e.getLocalizedMessage();
            builder.setErr(1).setMessage(message);
            logRpcResponse(messageId, message);
        } catch (Throwable e) {
            builder.setErr(2).setMessage("服务器错误");
            logRpcResponse(messageId, MockUtil.decodeThrowable(e));
        }
        sendServerResponse(session, builder.build(), messageId);
    }

    private static void sendServerResponse(Session session, CallbackData callback, int messageId) {
        Message message = Message.newBuilder()
                .setMessageType(Message.MessageType.RESPONSE)
                .setMessageId(messageId)
                .setCallback(callback)
                .build();
        session.send(message);
    }

    public static void removeRpcFuture(int mid) {
        futureMap.remove(mid);
    }

    /**
     * 处理rpc响应结果
     *
     * @param response 返回数据
     */
    public static void handleRpcResponse(Message response) {
        int mid = response.getMessageId();
        RpcFuture future = futureMap.remove(mid);
        if (future == null) {   // 异常数据，或者已经被超时丢弃
            return;
        }
        CallbackData callback = response.getCallback();
        int r = callback.getErr();
        if (r == 0) {
            future.complete(callback.getData());
            logRpcCallback(mid);
        } else if (r == 1) { // 逻辑异常
            String msg = callback.getMessage();
            future.completeExceptionally(new LogicException(msg));
            logRpcCallback(mid, msg);
        } else {
            future.completeExceptionally(new LogicException("请求错误")); // 服务器异常
            logRpcCallback(mid, callback.getMessage());
        }
    }

    /**
     * 底层异步调用RPC网络服务，等待响应结果
     */
    public static RpcFuture callRpcAsync(int serverId, String rpcName, String methodName, ForwardData forwardData) {
        Message msg = MockUtil.buildFromProto(forwardData, rpcName, methodName);
        int mid = msg.getMessageId();
        RpcFuture future = new RpcFuture(mid);
        // 超量跨服时可以控制一下吞吐量
        futureMap.put(mid, future);
        try {
            Session targetServer = MockUtil.getTargetServer(serverId);
            targetServer.send(msg);
        } catch (Throwable e) {
            futureMap.remove(mid);
            throw new ForwardException(serverId + "服务器不可达", e);
        }
        logRpcRequest(serverId, mid, rpcName, methodName);
        return future;
    }

    /**
     * 异步调用RPC网络服务，不关注响应结果
     */
    public static void callRpcNoBack(int serverId, String rpcName, String methodName, ForwardData forwardData) {
        Message msg = MockUtil.buildFromProto(forwardData, rpcName, methodName);
        int mid = msg.getMessageId();
        try {
            Session targetServer = MockUtil.getTargetServer(serverId);
            targetServer.send(msg);
        } catch (Throwable e) {
            throw new ForwardException(serverId + "服务器不可达", e);
        }
        logRpcRequest(serverId, mid, rpcName, methodName);
    }
}