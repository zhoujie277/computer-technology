package com.future.netty.chat.common.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class GroupJoinResponse extends Response {
    private Group group;


    public GroupJoinResponse(ResultCode code) {
        super(code);
    }

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_JOIN_ACK;
    }
}
