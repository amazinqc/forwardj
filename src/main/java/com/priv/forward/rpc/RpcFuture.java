package com.priv.forward.rpc;


import com.google.protobuf.ByteString;
import com.priv.forward.ForwardFuture;

/**
 * 异步转发的Future对象
 */
public class RpcFuture extends ForwardFuture<ByteString> {
    private final int mid;

    public RpcFuture(int mid) {
        this.mid = mid;
    }

    @Override
    protected void onTimeout() {
        ServerRpcManager.removeRpcFuture(mid);  // 同步超时从rpc集合中移除
    }

}
