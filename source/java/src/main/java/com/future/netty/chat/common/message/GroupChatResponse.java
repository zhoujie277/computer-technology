package com.future.netty.chat.common.message;

import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class GroupChatResponse extends Response {

    public GroupChatResponse(ResultCode resultCode) {
        super(resultCode);
    }

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_CHAT_ACK;
    }

}
