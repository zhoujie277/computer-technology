package com.future.netty.chat.common.message;

import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class ChatResponse extends Response {

    public ChatResponse(ResultCode resultCode) {
        super(resultCode);
    }

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.CHAT_ACK;
    }
}
