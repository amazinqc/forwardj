package com.priv.forward.mock.struct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.google.protobuf.ByteString;

public class MapEntry {


    private final ByteString key;
    private final ByteString value;

    public MapEntry(ByteString key, ByteString value) {
        this.key = key;
        this.value = value;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static MapEntry parseFrom(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        int length = bais.read();
        return new MapEntry(ByteString.copyFrom(bytes, 1, length), ByteString.copyFrom(bytes, length + 1, bytes.length - 1 - length));
    }

    public ByteString toByteString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(key.size());
        baos.write(key.toByteArray(), 0, key.size());
        baos.write(value.toByteArray(), 0, value.size());
        return ByteString.copyFrom(baos.toByteArray());
    }

    public ByteString getKey() {
        return key;
    }

    public ByteString getValue() {
        return value;
    }

    public static class Builder {
        private ByteString key;
        private ByteString value;

        private Builder() {
        }

        public Builder setKey(ByteString key) {
            this.key = key;
            return this;
        }

        public Builder setValue(ByteString value) {
            this.value = value;
            return this;
        }

        public MapEntry build() {
            return new MapEntry(key, value);
        }
    }
}
