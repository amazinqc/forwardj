package com.priv.forward.local;


import com.priv.forward.mock.LogicThread;

public class ForwardEventThread extends ForwardToThread {

    @Override
    protected LogicThread getTargetThread(Object[] args) {
        return LogicThread.getEventThread();
    }
}
