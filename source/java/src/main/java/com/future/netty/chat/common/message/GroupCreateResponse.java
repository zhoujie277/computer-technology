package com.future.netty.chat.common.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
public class GroupCreateResponse extends Response {

    private Group group;

    public GroupCreateResponse(ResultCode code) {
        super(code);
    }

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_CREATE_ACK;
    }
}