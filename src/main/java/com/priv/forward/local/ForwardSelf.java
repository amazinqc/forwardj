package com.priv.forward.local;


import com.priv.forward.Forward;
import com.priv.forward.ForwardException;
import com.priv.forward.ForwardHandle;
import com.priv.forward.ForwardMethod;
import com.priv.forward.mock.LogicException;

/**
 * 环路转发，相当于没有代理，直接调用执行
 */
public class ForwardSelf implements ForwardHandle {
    @Override
    public Object forward(Forward forward, ForwardMethod method, Object[] args) throws LogicException, ForwardException {
        return method.invokeNow(args);
    }
}
