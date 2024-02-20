package com.priv.forward.mock.net;

import com.google.protobuf.ByteString;

/**
 * RPC调用返回数据
 */
public class CallbackData {
    public static Builder newBuilder() {
        return new Builder();
    }

    private CallbackData(int err, ByteString data, String message) {
        this.err = err;
        this.data = data;
        this.message = message;
    }

    private final int err;
    private final ByteString data;
    private final String message;
    public ByteString getData() {
        return data;
    }

    public int getErr() {
        return err;
    }

    public String getMessage() {
        return message;
    }

    public static class Builder {
        private int err;
        private ByteString data;
        private String message;

        @SuppressWarnings("UnusedReturnValue")
        public Builder setData(ByteString bytes) {
            this.data = bytes;
            return this;
        }

        public Builder setErr(int err) {
            this.err = err;
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public CallbackData build() {
            return new CallbackData(err, data, message);
        }
    }
}
