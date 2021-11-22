package com.future.netty.chat.common.message;

import lombok.ToString;

@ToString(callSuper = true)
public class Pong extends Response {
    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.PONG;
    }
}