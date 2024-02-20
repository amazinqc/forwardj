package com.priv.forward.mock.struct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.google.protobuf.ByteString;

public class ForwardData {

    private final int index;
    private final ListByteArray args;

    private ForwardData(int index, ListByteArray args) {
        this.index = index;
        this.args = args;
    }

    public static ForwardData parseFrom(ByteString bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes.toByteArray());
        return new ForwardData(bais.read(), ListByteArray.parseFrom(bytes.substring(1)));
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public int getIndex() {
        return index;
    }

    public ListByteArray getArgs() {
        return args;
    }

    public ByteString toByteString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(index);
        byte[] bytes = args.toByteString().toByteArray();
        baos.write(bytes, 0, bytes.length);
        return ByteString.copyFrom(baos.toByteArray());
    }

    public static final class Builder {
        private int index;
        private ListByteArray args;

        private Builder() {
        }

        public Builder setIndex(int index) {
            this.index = index;
            return this;
        }

        public Builder setArgs(ListByteArray list) {
            this.args = list;
            return this;
        }

        public ForwardData build() {
            return new ForwardData(index, args);
        }
    }
}
