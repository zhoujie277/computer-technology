package com.future.netty.chat.common.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class GroupJoinRequest extends Message {
    private String groupID;
    private User fromUser;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_JOIN_REQ;
    }
}
