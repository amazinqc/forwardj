package com.priv.forward.rpc.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.priv.forward.mock.MockUtil;
import com.priv.forward.mock.struct.ListByteArray;

@SuppressWarnings("unused")
public enum MethodArgs implements ProtobufConverter {

    VoidType(void.class, Void.class) {
        @Override
        public ByteString withNonNull(Object arg) {
            return ByteString.empty();
        }

        @Override
        public Object fromByteString(ByteString arg, Type type, Class<?> argType) {
            return null;
        }
    },
    BooleanType(boolean.class, Boolean.class) {
        @Override
        public ByteString withNonNull(Object arg) {
            return BoolValue.of((boolean) arg).toByteString();
        }

        @Override
        public Object fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException {
            return BoolValue.parseFrom(arg).getValue();
        }
    },
    IntType(int.class, Integer.class) {
        @Override
        public ByteString withNonNull(Object arg) {
            return Int32Value.of((int) arg).toByteString();
        }

        @Override
        public Object fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException {
            return Int32Value.parseFrom(arg).getValue();
        }
    },
    LongType(long.class, Long.class) {
        @Override
        public ByteString withNonNull(Object arg) {
            return Int64Value.of((long) arg).toByteString();
        }

        @Override
        public Object fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException {
            return Int64Value.parseFrom(arg).getValue();
        }
    },
    StringType(String.class) {
        @Override
        public ByteString withNonNull(Object arg) {
            return StringValue.of((String) arg).toByteString();
        }

        @Override
        public String fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException {
            return StringValue.parseFrom(arg).getValue();
        }
    },
    ProtobufType() {
        @Override
        public ByteString withNonNull(Object arg) {
            return ((GeneratedMessageV3) arg).toByteString();
        }

        @Override
        public Object fromByteString(ByteString arg, Type type, Class<?> argType) throws InvalidProtocolBufferException {
            try {
                return argType.getMethod("parseFrom", ByteString.class).invoke(null, arg);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new InvalidProtocolBufferException(e);  // 错误的protobuf类型
            }
        }
    },
    POJOType() {    // 简单java类型改用ObjectStream序列化和反序列化(Serializable)，也可以扩展自动适配

        @Override
        public ByteString withNonNull(Object arg) {
            try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                 ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
                objectStream.writeObject(arg);
                return ByteString.copyFrom(byteStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object fromByteString(ByteString arg, Type type, Class<?> argType) {
            if (arg.isEmpty()) {
                return null;
            }
            try (ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(arg.toByteArray()))) {
                return objectStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };

    MethodArgs(Class<?>... registerTypes) {
        for (Class<?> registerType : registerTypes) {
            Registry.typeMap.put(registerType, this);
        }
    }

    public static ListByteArray buildProtobuf(Parameter[] parameters, Object[] args) {
        if (args.length != parameters.length) {
            throw new IllegalArgumentException("参数数量错误");
        }
        ListByteArray.Builder builder = ListByteArray.newBuilder();
        for (int i = 0; i < args.length; i++) {
            Parameter parameter = parameters[i];
            Converter converter = parameter.getAnnotatedType().getAnnotation(Converter.class);
            builder.addElements(buildByteString(parameter.getParameterizedType(), parameter.getType(), args[i], converter));
        }
        return builder.build();
    }

    public static Object[] buildObjectArgs(Parameter[] parameters, ListByteArray argArray) throws InvalidProtocolBufferException {
        if (argArray.getElementsCount() != parameters.length) {
            throw new IllegalArgumentException("参数数量错误");
        }
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < args.length; i++) {
            Parameter parameter = parameters[i];
            Converter converter = parameter.getAnnotatedType().getAnnotation(Converter.class);
            args[i] = buildObject(parameter.getParameterizedType(), parameter.getType(), argArray.getElements(i), converter);
        }
        return args;
    }

    public static Object buildObject(Type type, ByteString bytes, Converter converter) throws InvalidProtocolBufferException {
        return buildObject(type, getRawType(type), bytes, converter);
    }

    public static ByteString buildByteString(Type type, Object value, Converter converter) {
        return buildByteString(type, getRawType(type), value, converter);
    }

    private static ByteString buildByteString(Type type, Class<?> rawType, Object value, Converter param) {
        Class<? extends ProtobufConverter> converter = param == null ? DefaultConverter.class : param.value();
        return MockUtil.getInstance(converter).toByteString(value, type, rawType);
    }

    private static Object buildObject(Type type, Class<?> rawType, ByteString bytes, Converter param) throws InvalidProtocolBufferException {
        Class<? extends ProtobufConverter> converter = param == null ? DefaultConverter.class : param.value();
        return MockUtil.getInstance(converter).fromByteString(bytes, type, rawType);
    }

    public static ProtobufConverter getArgConverter(Class<?> type) {
        ProtobufConverter converter = Registry.typeMap.get(type);
        if (converter == null) {
            if (GeneratedMessageV3.class.isAssignableFrom(type)) {
                converter = MethodArgs.ProtobufType;
            } else {
                converter = MethodArgs.POJOType;
            }
        }
        return converter;
    }

    /**
     * Class类型转换
     */
    public static Class<?> classType(Type type) {
        try {
            return (Class<?>) type;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("当前不支持特殊类型：" + type);
        }
    }

    /**
     * 获取原始类型Class
     */
    public static Class<?> getRawType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return classType(parameterizedType.getRawType());
        } else if (type instanceof Class) {
            return (Class<?>) type;
        } else {
            throw new IllegalArgumentException("暂不支持的泛型解析" + type);
        }
    }

    protected abstract ByteString withNonNull(Object arg);

    protected ByteString withNull() {
        return ByteString.empty();
    }

    public ByteString toByteString(Object arg, Type type, Class<?> argType) {
        if (arg == null) {
            return withNull();
        }
        return withNonNull(arg);
    }

    private static class Registry {
        private static final Map<Class<?>, ProtobufConverter> typeMap = new HashMap<>();
    }
}
