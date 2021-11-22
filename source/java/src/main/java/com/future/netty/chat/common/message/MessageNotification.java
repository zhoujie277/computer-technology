package com.future.netty.chat.common.message;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MessageNotification extends Message {

    @Override
    public MsgType getMessageType() {
        return Message.MsgType.MESSAGE_NOTIFICATION;
    }

    private long msgID;
    private String json;
    private String timestamp;
}
