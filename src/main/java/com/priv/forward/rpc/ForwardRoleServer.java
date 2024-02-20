package com.priv.forward.rpc;


import com.priv.forward.ForwardException;
import com.priv.forward.mock.MockUtil;

/**
 * 转发到玩家的角色服务器
 */
public class ForwardRoleServer extends ForwardToServer {

    @Override
    protected int getServerId(Object[] args) {
        if (args.length < 1 || (!(args[0] instanceof Long))) {
            throw new ForwardException("缺少uid转发参数");
        }
        long uid = (long) args[0];
        int sid = MockUtil.getUserSid(uid);
        if (sid == 0) {
            throw new ForwardException("未知的角色uid[" + uid + "]");
        }
        return sid;
    }
}
