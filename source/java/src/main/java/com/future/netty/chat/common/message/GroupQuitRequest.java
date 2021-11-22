package com.future.netty.chat.common.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class GroupQuitRequest extends Message {

    private String groupID;

    private User fromUser;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_QUIT_REQ;
    }

}
