package com.priv.forward;


import com.priv.forward.mock.LogicException;

/**
 * 转发处理策略
 * <p>
 * 目前使用forwards遍历策略处理转发逻辑；
 * <p>暂时不允许rpc目标服务器进行二次代理，后续可以扩展rpc协议头信息，防止转发循环。
 */
public interface ForwardHandle {

    /**
     * 转发（主动方）
     */
    Object forward(Forward forward, ForwardMethod method, Object[] args) throws LogicException, ForwardException;

}
