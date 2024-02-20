package com.priv.forward.rpc;


import com.priv.forward.mock.MockUtil;

public class ForwardCentralServer extends ForwardToServer {

    @Override
    protected int getServerId(Object[] args) {
        return MockUtil.getCentralServerId();
    }
}
