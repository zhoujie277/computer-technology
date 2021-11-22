package com.future.netty.chat.common.message;

public class Ping extends Message {
    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.PING;
    }
}
