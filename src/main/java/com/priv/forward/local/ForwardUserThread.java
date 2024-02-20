package com.priv.forward.local;


import com.priv.forward.ForwardException;
import com.priv.forward.mock.LogicThread;

/**
 * uid玩家线程转发
 */
public class ForwardUserThread extends ForwardToThread {

    @Override
    protected LogicThread getTargetThread(Object[] args) {
        if (args.length < 1 || (!(args[0] instanceof Long))) {
            throw new ForwardException("缺少uid转发参数");
        }

        long uid = (long) args[0];
        return LogicThread.getLogicThread(uid);
    }
}
