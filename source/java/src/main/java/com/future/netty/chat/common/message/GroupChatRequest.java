package com.future.netty.chat.common.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class GroupChatRequest extends Message {
    private String groupID;
    private User fromUser;
    private Message.ContentType contentType;
    private String content;
    private long time;

    @Override
    public Message.MsgType getMessageType() {
        return Message.MsgType.GROUP_CHAT_REQ;
    }
}
