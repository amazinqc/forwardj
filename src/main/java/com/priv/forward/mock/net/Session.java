package com.priv.forward.mock.net;

import com.priv.forward.rpc.ServerRpcManager;

/**
 * 远端链接对象抽象
 */
public class Session {

    public void send(Message message) {
        switch (message.getMessageType()) {
            case REQUEST:
                ServerRpcManager.handleRpcRequest(this, message);
                break;
            case RESPONSE:
                ServerRpcManager.handleRpcResponse(message);
                break;
            default:
                break;
        }
    }
}
