package com.priv.forward.rpc.converter;

import java.lang.reflect.Type;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.priv.forward.mock.MockUtil;
import com.priv.forward.mock.struct.ParamInfo;

public class GenericTypeConverter extends DefaultConverter {
    @Override
    public ByteString toByteString(Object arg, Type type, Class<?> argType) {
        if (arg == null) {
            return ByteString.empty();
        }
        argType = arg.getClass();
        return ParamInfo.newBuilder().setName(argType.getName()).setData(super.toByteString(arg, type, argType)).build().toByteString();
    }

    @Override
    public Object fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException {
        ParamInfo paramInfo = ParamInfo.parseFrom(arg);
        arg = paramInfo.getData();
        Class<?> clazz = MockUtil.forName(paramInfo.getName());
        if (clazz != null) {
            argType = clazz;
        }
        return super.fromByteString(arg, type, argType);
    }
}
