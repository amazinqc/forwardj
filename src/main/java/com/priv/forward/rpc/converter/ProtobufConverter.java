package com.priv.forward.rpc.converter;

import java.lang.reflect.Type;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public interface ProtobufConverter {

    ByteString toByteString(Object arg, Type type, Class<?> argType);

    Object fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException;


}
