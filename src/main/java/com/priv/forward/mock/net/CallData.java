package com.priv.forward.mock.net;

import com.google.protobuf.ByteString;

/**
 * RPC调用数据
 */
public class CallData {

    private final String forwardName;
    private final String methodName;
    private final ByteString data;

    private CallData(String forwardName, String methodName, ByteString data) {
        this.forwardName = forwardName;
        this.methodName = methodName;
        this.data = data;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getForwardName() {
        return forwardName;
    }

    public String getMethodName() {
        return methodName;
    }

    public ByteString getData() {
        return data;
    }

    public static class Builder {

        private String forwardName;
        private String methodName;
        private ByteString data;

        public Builder setForwardName(String forwardName) {
            this.forwardName = forwardName;
            return this;
        }

        public Builder setMethodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder setData(ByteString bytes) {
            this.data = bytes;
            return this;
        }

        public CallData build() {
            return new CallData(forwardName, methodName, data);
        }
    }
}
