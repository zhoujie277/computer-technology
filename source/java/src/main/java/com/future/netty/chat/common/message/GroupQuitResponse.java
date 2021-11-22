package com.future.netty.chat.common.message;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class GroupQuitResponse extends Response {
    public GroupQuitResponse(ResultCode code) {
        super(code);
    }

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_QUIT_ACK;
    }
}