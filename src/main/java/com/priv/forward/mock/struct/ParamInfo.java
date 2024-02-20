package com.priv.forward.mock.struct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.google.protobuf.ByteString;

/**
 * RPC参数的扩展结构
 */
public class ParamInfo {

    private final String name;
    private final ByteString data;

    public ParamInfo(String name, ByteString data) {
        this.name = name;
        this.data = data;
    }

    public static ParamInfo parseFrom(ByteString bytes) {
        byte[] array = bytes.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(array);
        int read = bais.read();
        return new ParamInfo(new String(array, 1, read), ByteString.copyFrom(array, 1 + read, array.length - 1 - read));
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public ByteString toByteString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = name.getBytes();
        baos.write(bytes.length);
        baos.write(bytes, 0, bytes.length);
        baos.write(data.toByteArray(), 0, data.size());
        return ByteString.copyFrom(baos.toByteArray());
    }

    public ByteString getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public static class Builder {

        private String name;
        private ByteString data;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setData(ByteString data) {
            this.data = data;
            return this;
        }

        public ParamInfo build() {
            return new ParamInfo(name, data);
        }
    }
}
