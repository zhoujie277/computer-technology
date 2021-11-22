package com.future.netty.chat.common.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class GroupCreateRequest extends Request {
    private static final long serialVersionUID = 1905142041750251207L;

    private Group group;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_CREATE_REQ;
    }

}
