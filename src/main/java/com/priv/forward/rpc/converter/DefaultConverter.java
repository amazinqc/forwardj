package com.priv.forward.rpc.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.priv.forward.mock.struct.ListByteArray;
import com.priv.forward.mock.struct.MapByteArray;
import com.priv.forward.mock.struct.MapEntry;

public class DefaultConverter implements ProtobufConverter {

    protected static Object wrap(Type type, ByteString value, Collection<Object> collection) throws InvalidProtocolBufferException {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type elementType = parameterizedType.getActualTypeArguments()[0];
            Class<?> rawType = MethodArgs.getRawType(elementType);
            ProtobufConverter converter = MethodArgs.getArgConverter(rawType);
            PBFunction<ByteString, Object> handle;
            if (converter == MethodArgs.POJOType) {
                if (List.class.isAssignableFrom(rawType)) {
                    handle = bytes -> wrap(elementType, bytes, new ArrayList<>());
                } else if (Set.class.isAssignableFrom(rawType)) {
                    handle = bytes -> wrap(elementType, bytes, new HashSet<>());
                } else if (Map.class.isAssignableFrom(rawType)) {
                    handle = bytes -> wrap(elementType, bytes, new HashMap<>());
                } else {
                    handle = bytes -> converter.fromByteString(bytes, type, rawType);
                }
            } else {
                handle = bytes -> converter.fromByteString(bytes, type, rawType);
            }
            ListByteArray list = ListByteArray.parseFrom(value);
            for (ByteString bytes : list.getElementsList()) {
                collection.add(handle.apply(bytes));
            }
            return collection;
        }
        throw new IllegalArgumentException("不支持的参数类型：" + type);
    }

    protected static Object wrap(Type type, ByteString value, Map<Object, Object> map) throws InvalidProtocolBufferException {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] arguments = parameterizedType.getActualTypeArguments();
            Type keyType = arguments[0];
            Type valType = arguments[1];
            Class<?> keyRawType = MethodArgs.classType(keyType);
            Class<?> valRawType = MethodArgs.classType(valType);
            ProtobufConverter keyConverter = MethodArgs.getArgConverter(keyRawType);
            ProtobufConverter valConverter = MethodArgs.getArgConverter(valRawType);
            PBFunction<ByteString, Object> valHandle;
            if (valConverter == MethodArgs.POJOType) {
                if (List.class.isAssignableFrom(valRawType)) {
                    valHandle = bytes -> wrap(valType, bytes, new ArrayList<>());
                } else if (Set.class.isAssignableFrom(valRawType)) {
                    valHandle = bytes -> wrap(valType, bytes, new HashSet<>());
                } else if (Map.class.isAssignableFrom(valRawType)) {
                    valHandle = bytes -> wrap(valType, bytes, new HashMap<>());
                } else {
                    valHandle = bytes -> valConverter.fromByteString(bytes, type, valRawType);
                }
            } else {
                valHandle = bytes -> valConverter.fromByteString(bytes, type, valRawType);
            }
            MapByteArray from = MapByteArray.parseFrom(value);
            for (MapEntry entry : from.getEntryList()) {
                map.put(keyConverter.fromByteString(entry.getKey(), type, keyRawType), valHandle.apply(entry.getValue()));
            }
            return map;
        }
        throw new IllegalArgumentException("不支持的参数类型：" + type);
    }

    protected static ByteString unwrap(Type type, Collection<?> iter) {
        if (iter == null) {
            return ByteString.empty();
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type elementType = parameterizedType.getActualTypeArguments()[0];
            Class<?> rawType = MethodArgs.getRawType(elementType);
            ProtobufConverter converter = MethodArgs.getArgConverter(rawType);
            Function<Object, ByteString> handle;
            if (converter == MethodArgs.POJOType) { // 对值判断复杂类型中的集合对象，判断前置
                if (List.class.isAssignableFrom(rawType) || Set.class.isAssignableFrom(rawType)) {
                    handle = val -> unwrap(elementType, (Collection<?>) val);
                } else if (Map.class.isAssignableFrom(rawType)) {
                    handle = val -> unwrap(elementType, (Map<?, ?>) val);
                } else {
                    handle = arg -> converter.toByteString(arg, type, rawType);
                }
            } else {
                handle = arg -> converter.toByteString(arg, type, rawType);
            }
            ListByteArray.Builder builder = ListByteArray.newBuilder();
            for (Object value : iter) {
                assert value.getClass() == rawType : "集合元素类型不一致，期望的参数类型：" + rawType + "，实际的参数：" + value;
                builder.addElements(handle.apply(value));
            }
            return builder.build().toByteString();
        }
        throw new IllegalArgumentException("不支持的参数类型：" + type);
    }

    protected static ByteString unwrap(Type type, Map<?, ?> map) {
        if (map == null) {
            return ByteString.empty();
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] arguments = parameterizedType.getActualTypeArguments();
            Type keyType = arguments[0];
            Type valType = arguments[1];
            Class<?> keyRawType = MethodArgs.getRawType(keyType);
            Class<?> valRawType = MethodArgs.getRawType(valType);
            ProtobufConverter keyConverter = MethodArgs.getArgConverter(keyRawType);
            ProtobufConverter valConverter = MethodArgs.getArgConverter(valRawType);
            MapByteArray.Builder builder = MapByteArray.newBuilder();
            Function<Object, ByteString> valHandle;
            if (valConverter == MethodArgs.POJOType) { // 只对value值判断复杂类型中的集合对象，判断前置
                if (List.class.isAssignableFrom(valRawType) || Set.class.isAssignableFrom(valRawType)) {
                    valHandle = val -> unwrap(valType, (Collection<?>) val);
                } else if (Map.class.isAssignableFrom(valRawType)) {
                    valHandle = val -> unwrap(valType, (Map<?, ?>) val);
                } else {
                    valHandle = arg -> valConverter.toByteString(arg, valType, valRawType);
                }
            } else {
                valHandle = arg -> valConverter.toByteString(arg, valType, valRawType);
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                assert key.getClass() == keyRawType : "集合元素类型不一致，期望的参数类型：" + keyRawType + "，实际的参数：" + key;
                assert val.getClass() == valRawType : "集合元素类型不一致，期望的参数类型：" + valRawType + "，实际的参数：" + val;
                builder.addEntry(MapEntry.newBuilder()
                        .setKey(keyConverter.toByteString(key, keyType, keyRawType))
                        .setValue(valHandle.apply(val)));
            }
            return builder.build().toByteString();
        }
        throw new IllegalArgumentException("不支持的参数类型：" + type);
    }

    @Override
    public ByteString toByteString(Object arg, Type type, Class<?> argType) {
        ProtobufConverter converter = MethodArgs.getArgConverter(argType);

        if (converter == MethodArgs.POJOType) { // 对复杂类型中的集合对象，做一层拆解
            if (List.class.isAssignableFrom(argType) || Set.class.isAssignableFrom(argType)) {
                return unwrap(type, (Collection<?>) arg);
            }
            if (Map.class.isAssignableFrom(argType)) {
                return unwrap(type, (Map<?, ?>) arg);
            }
        }
        return converter.toByteString(arg, type, argType);
    }

    @Override
    public Object fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException {
        ProtobufConverter converter = MethodArgs.getArgConverter(argType);
        if (converter == MethodArgs.POJOType) {  // 对复杂类型中的集合对象，做一层包装
            if (List.class.isAssignableFrom(argType)) {
                return wrap(type, arg, new ArrayList<>());
            }
            if (Set.class.isAssignableFrom(argType)) {
                return wrap(type, arg, new HashSet<>());
            }
            if (Map.class.isAssignableFrom(argType)) {
                return wrap(type, arg, new HashMap<>());
            }
        }
        return converter.fromByteString(arg, type, argType);
    }

    protected interface PBFunction<T, R> {
        R apply(T t) throws InvalidProtocolBufferException;
    }
}
