package com.priv.forward.mock.net;

/**
 * 网络通信协议结构
 */
public class Message {

    private final MessageType type;
    private final CallData call;
    private final CallbackData callback;

    public Message(MessageType type, CallData call, CallbackData callback) {
        this.type = type;
        this.call = call;
        this.callback = callback;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public CallbackData getCallback() {
        return callback;
    }

    public int getMessageId() {
        return 0;
    }

    public CallData getCall() {
        return call;
    }

    public MessageType getMessageType() {
        return type;
    }

    public enum MessageType {
        /**
         * 请求
         */
        REQUEST,
        /**
         * 响应
         */
        RESPONSE,


    }

    public static class Builder {

        private MessageType type;
        private CallbackData callback;
        private CallData call;

        public Builder setMessageId(int ignoredMessageId) {
            return this;
        }

        public Builder setMessageType(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder setCall(CallData call) {
            this.call = call;
            return this;
        }

        public Builder setCallback(CallbackData callback) {
            this.callback = callback;
            return this;
        }

        public Message build() {
            return new Message(type, call, callback);
        }
    }
}
